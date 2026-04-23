import type { FetchTask, Product, Platform } from "@/types";

const TASK_TIMEOUT = 30000;
const pendingFetches = new Map<string, { tabId: number; timeoutId: number }>();

const STORAGE_KEYS = {
  PRODUCT_LIST: "productList",
  TASK_QUEUE: "taskQueue",
} as const;

async function getStorage<T>(key: string, defaultValue: T): Promise<T> {
  const result = await chrome.storage.local.get(key);
  return result[key] ?? defaultValue;
}

async function setStorage<T>(key: string, value: T): Promise<void> {
  await chrome.storage.local.set({ [key]: value });
}

async function getTaskQueue(): Promise<FetchTask[]> {
  return getStorage<FetchTask[]>(STORAGE_KEYS.TASK_QUEUE, []);
}

async function saveTaskQueue(tasks: FetchTask[]): Promise<void> {
  await setStorage(STORAGE_KEYS.TASK_QUEUE, tasks);
}

async function getProducts(): Promise<Product[]> {
  return getStorage<Product[]>(STORAGE_KEYS.PRODUCT_LIST, []);
}

async function isProductOrTaskExists(
  title: string,
  url: string,
): Promise<boolean> {
  const [products, tasks] = await Promise.all([getProducts(), getTaskQueue()]);
  const productExists = products.some(
    (p) => p.productName === title || p.sourceUrl === url,
  );
  const taskExists = tasks.some((t) => t.title === title || t.url === url);
  return productExists || taskExists;
}

function detectPlatform(url: string): Platform {
  if (url.includes("taobao.com")) return "taobao";
  if (url.includes("tmall.com")) return "tmall";
  if (url.includes("1688.com")) return "1688";
  return "taobao";
}

async function addTask(task: FetchTask): Promise<void> {
  const tasks = await getTaskQueue();
  tasks.push(task);
  await saveTaskQueue(tasks);
}

async function updateTask(
  taskId: string,
  updates: Partial<FetchTask>,
): Promise<void> {
  const tasks = await getTaskQueue();
  const index = tasks.findIndex((t) => t.id === taskId);
  if (index !== -1) {
    tasks[index] = { ...tasks[index], ...updates };
    await saveTaskQueue(tasks);
  }
}

async function removeTask(taskId: string): Promise<void> {
  const tasks = await getTaskQueue();
  await saveTaskQueue(tasks.filter((t) => t.id !== taskId));
}

async function clearAllTasks(): Promise<void> {
  await saveTaskQueue([]);
  pendingFetches.forEach(({ timeoutId }) => clearTimeout(timeoutId));
  pendingFetches.clear();
}

function startTaskTimeout(taskId: string, tabId: number): void {
  const timeoutId = setTimeout(async () => {
    console.warn(`Task ${taskId} timed out`);
    await updateTask(taskId, { status: "failed" });
    pendingFetches.delete(taskId);
  }, TASK_TIMEOUT);
  pendingFetches.set(taskId, { tabId, timeoutId });
}

function clearTaskTimeout(taskId: string): void {
  const pending = pendingFetches.get(taskId);
  if (pending) {
    clearTimeout(pending.timeoutId);
    pendingFetches.delete(taskId);
  }
}

async function handleStartFetch(
  title: string,
  url: string,
  tabId: number,
  sendResponse: (response: unknown) => void,
): Promise<void> {
  const exists = await isProductOrTaskExists(title, url);
  if (exists) {
    sendResponse({ status: "error", message: "该商品已存在或正在获取中" });
    return;
  }

  const platform = detectPlatform(url);
  const task: FetchTask = {
    id: url,
    title,
    url,
    platform,
    status: "pending",
    totalVariants: 0,
    currentVariants: 0,
    progress: 0,
    startTime: Date.now(),
  };
  console.log("task", task);
  await addTask(task);
  startTaskTimeout(task.id, tabId);

  try {
    await chrome.tabs.sendMessage(tabId, {
      type: "START_FETCH",
      payload: { taskId: task.id },
    });
    await updateTask(task.id, { status: "fetching" });
    sendResponse({ status: "ok", taskId: task.id });
  } catch (error) {
    console.error("Failed to send message to content script:", error);
    await updateTask(task.id, { status: "failed" });
    clearTaskTimeout(task.id);
    sendResponse({ status: "error", message: "无法连接到目标页面" });
  }
}

async function handleFetchProgress(
  taskId: string,
  totalVariants: number,
  currentVariants: number,
): Promise<void> {
  const progress =
    totalVariants > 0 ? Math.round((currentVariants / totalVariants) * 100) : 0;
  await updateTask(taskId, { totalVariants, currentVariants, progress });
}

async function handleFetchComplete(
  taskId: string,
  product: Product,
): Promise<void> {
  console.log("handleFetchComplete", product);
  clearTaskTimeout(taskId);
  const tasks = await getTaskQueue();
  const task = tasks.find((t) => t.id === taskId);

  if (!task) {
    console.warn(`Task ${taskId} not found in queue, skipping save`);
    return;
  }

  const products = await getProducts();
  products.push(product);
  await setStorage(STORAGE_KEYS.PRODUCT_LIST, products);

  await updateTask(taskId, { status: "completed", progress: 100 });

  setTimeout(() => removeTask(taskId), 2000);
}

async function handleFetchError(
  taskId: string,
  errorMessage: string,
): Promise<void> {
  clearTaskTimeout(taskId);
  await updateTask(taskId, { status: "failed" });
  console.error(`Task ${taskId} failed:`, errorMessage);
}

chrome.runtime.onInstalled.addListener(() => {
  console.log("PurchaseX extension installed");
});

chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  const tabId = message.payload?.tabId || sender.tab?.id;

  switch (message.type) {
    case "START_FETCH":
      if (tabId && message.payload?.title && message.payload?.url) {
        handleStartFetch(
          message.payload.title,
          message.payload.url,
          tabId,
          sendResponse,
        );
        return true;
      }
      sendResponse({ status: "error", message: "参数不完整" });
      break;

    case "FETCH_PROGRESS":
      console.log("FETCH_PROGRESS", message.payload);
      if (
        message.payload?.taskId &&
        message.payload?.totalVariants !== undefined
      ) {
        handleFetchProgress(
          message.payload.taskId,
          message.payload.totalVariants,
          message.payload.currentVariants || 0,
        ).then(() => sendResponse({ status: "ok" }));
        return true;
      }
      sendResponse({ status: "error", message: "参数不完整" });
      return true;

    case "FETCH_COMPLETE":
      console.log("FETCH_COMPLETE", message.payload);
      if (message.payload?.taskId && message.payload?.product) {
        handleFetchComplete(
          message.payload.taskId,
          message.payload.product,
        ).then(() => sendResponse({ status: "ok" }));
        return true;
      }
      sendResponse({ status: "error", message: "参数不完整" });
      return true;

    case "FETCH_ERROR":
      console.log("FETCH_ERROR", message.payload);
      if (message.payload?.taskId) {
        handleFetchError(
          message.payload.taskId,
          message.payload.errorMessage || "Unknown error",
        ).then(() => sendResponse({ status: "ok" }));
        return true;
      }
      sendResponse({ status: "error", message: "参数不完整" });
      return true;

    case "CANCEL_ALL_TASKS":
      clearAllTasks().then(() => sendResponse({ status: "ok" }));
      return true;

    case "GET_TASK_QUEUE":
      getTaskQueue().then((tasks) => sendResponse({ status: "ok", tasks }));
      return true;

    default:
      sendResponse({ status: "error", message: "Unknown message type" });
  }

  return true;
});

chrome.tabs.onRemoved.addListener((tabId) => {
  pendingFetches.forEach((pending, taskId) => {
    if (pending.tabId === tabId) {
      clearTaskTimeout(taskId);
      updateTask(taskId, { status: "failed" });
      console.warn(`Tab ${tabId} closed, task ${taskId} marked as failed`);
    }
  });
});
