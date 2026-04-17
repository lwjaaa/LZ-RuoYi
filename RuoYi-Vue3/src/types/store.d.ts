declare module "@/store/modules/user.js" {
  import type { defineStore } from "pinia";

  interface UserState {
    token: string;
    id: number | null;
    name: string;
    avatar: string;
    roles: string[];
    permissions: string[];
  }

  interface UserStore {
    token: string;
    id: number | null;
    name: string;
    avatar: string;
    roles: string[];
    permissions: string[];
    login: (userInfo: { username: string; password: string; code: string; uuid: string }) => Promise<void>;
    logout: () => Promise<void>;
    getInfo: () => Promise<void>;
    resetToken: () => void;
  }

  export const useUserStore: ReturnType<typeof defineStore<"user", UserStore>>;
}

declare module "@/store/modules/app.js" {
  import type { defineStore } from "pinia";

  interface AppState {
    sidebar: {
      opened: boolean;
      withoutAnimation: boolean;
      hide: boolean;
    };
    device: string;
    size: string;
  }

  interface AppStore {
    sidebar: AppState["sidebar"];
    device: string;
    size: string;
    toggleSideBar: (withoutAnimation?: boolean) => void;
    closeSideBar: (withoutAnimation: boolean) => void;
    toggleDevice: (device: string) => void;
    setSize: (size: string) => void;
  }

  export const useAppStore: ReturnType<typeof defineStore<"app", AppStore>>;
}

declare module "@/store/modules/permission.js" {
  import type { defineStore } from "pinia";
  import type { RouteRecordRaw } from "vue-router";

  interface PermissionState {
    routes: RouteRecordRaw[];
    addRoutes: RouteRecordRaw[];
    defaultRoutes: RouteRecordRaw[];
    topbarRouters: RouteRecordRaw[];
    sidebarRouters: RouteRecordRaw[];
  }

  interface PermissionStore {
    routes: RouteRecordRaw[];
    addRoutes: RouteRecordRaw[];
    defaultRoutes: RouteRecordRaw[];
    topbarRouters: RouteRecordRaw[];
    sidebarRouters: RouteRecordRaw[];
    setRoutes: (routes: RouteRecordRaw[]) => void;
    setDefaultRoutes: (routes: RouteRecordRaw[]) => void;
    setTopbarRoutes: (routes: RouteRecordRaw[]) => void;
    setSidebarRouters: (routes: RouteRecordRaw[]) => void;
    generateRoutes: (roles: string[]) => Promise<RouteRecordRaw[]>;
  }

  export const usePermissionStore: ReturnType<typeof defineStore<"permission", PermissionStore>>;
}

declare module "@/store/modules/tagsView.js" {
  import type { defineStore } from "pinia";

  interface TagView {
    path: string;
    name?: string;
    title: string;
    meta?: Record<string, any>;
    query?: Record<string, string>;
    fullPath?: string;
    affix?: boolean;
  }

  interface TagsViewState {
    visitedViews: TagView[];
    cachedViews: string[];
    iframeViews: TagView[];
  }

  interface TagsViewStore {
    visitedViews: TagView[];
    cachedViews: string[];
    iframeViews: TagView[];
    addView: (view: TagView) => void;
    addVisitedView: (view: TagView) => void;
    addCachedView: (view: TagView) => void;
    addIframeView: (view: TagView) => void;
    delView: (view: TagView) => Promise<TagView[]>;
    delVisitedView: (view: TagView) => Promise<TagView[]>;
    delCachedView: (view: TagView) => Promise<string[]>;
    delIframeView: (view: TagView) => Promise<TagView[]>;
    delOthersViews: (view: TagView) => Promise<{ visitedViews: TagView[]; cachedViews: string[] }>;
    delAllViews: () => Promise<{ visitedViews: TagView[]; cachedViews: string[] }>;
    delAllCachedViews: () => Promise<string[]>;
    updateVisitedView: (view: TagView) => void;
    delRightTags: (view: TagView) => Promise<TagView[]>;
    delLeftTags: (view: TagView) => Promise<TagView[]>;
  }

  export const useTagsViewStore: ReturnType<typeof defineStore<"tagsView", TagsViewStore>>;
}

declare module "@/store/modules/settings.js" {
  import type { defineStore } from "pinia";

  interface SettingsState {
    title: string;
    theme: string;
    sideTheme: string;
    showSettings: boolean;
    topNav: boolean;
    tagsView: boolean;
    fixedHeader: boolean;
    sidebarLogo: boolean;
    dynamicTitle: boolean;
  }

  interface SettingsStore extends SettingsState {
    changeSetting: (data: { key: keyof SettingsState; value: any }) => void;
  }

  export const useSettingsStore: ReturnType<typeof defineStore<"settings", SettingsStore>>;
}

declare module "@/store/modules/dict.js" {
  import type { defineStore } from "pinia";

  interface DictData {
    dictValue: string;
    dictLabel: string;
    cssClass?: string;
    listClass?: string;
    isDefault?: string;
    status?: string;
    default?: boolean;
    remark?: string;
  }

  interface DictState {
    dict: Record<string, DictData[]>;
  }

  interface DictStore {
    dict: Record<string, DictData[]>;
    getDict: (key: string) => DictData[];
    setDict: (key: string, value: DictData[]) => void;
    removeDict: (key: string) => boolean;
    cleanDict: () => void;
  }

  export const useDictStore: ReturnType<typeof defineStore<"dict", DictStore>>;
}

declare module "@/store/modules/lock.js" {
  import type { defineStore } from "pinia";

  interface LockState {
    isLocked: boolean;
    lockPassword: string;
  }

  interface LockStore {
    isLocked: boolean;
    lockPassword: string;
    lock: (password: string) => void;
    unlock: (password: string) => boolean;
  }

  export const useLockStore: ReturnType<typeof defineStore<"lock", LockStore>>;
}
