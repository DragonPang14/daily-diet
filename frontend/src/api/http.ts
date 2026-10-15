/**
 * HTTP 客户端封装：基于 uni.request，对接后端 /api/v1 接口
 * - JWT 自动注入（从 storage 读取）
 * - 统一解包 ResponseResult { code, msg, data }，code=0 返回 data
 * - 401 自动清 token + 跳转 onboarding
 */

export interface ResponseResult<T> {
  code: number;
  msg: string;
  data: T;
}

const TOKEN_KEY = "token";
const ONBOARDING_PAGE = "/pages/onboarding/index";

function getBase(): string {
  return import.meta.env.VITE_API_BASE || "/api";
}

function buildHeader(custom?: Record<string, string>): Record<string, string> {
  const token = uni.getStorageSync(TOKEN_KEY) as string;
  const header: Record<string, string> = {
    "Content-Type": "application/json",
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
  return { ...header, ...(custom || {}) };
}

export function request<T>(opts: UniApp.RequestOptions): Promise<T> {
  return new Promise((resolve, reject) => {
    uni.request({
      ...opts,
      url: getBase() + opts.url,
      header: buildHeader(opts.header as Record<string, string>),
      success: (res) => {
        if (res.statusCode === 401) {
          uni.removeStorageSync(TOKEN_KEY);
          uni.reLaunch({ url: ONBOARDING_PAGE });
          reject(new Error("未登录"));
          return;
        }
        const r = res.data as ResponseResult<T>;
        if (r && typeof r.code === "number" && r.code === 0) {
          resolve(r.data);
          return;
        }
        const msg = (r && r.msg) || `请求失败 (${res.statusCode})`;
        uni.showToast({ title: msg, icon: "none" });
        reject(new Error(msg));
      },
      fail: (err) => {
        uni.showToast({ title: "网络异常", icon: "none" });
        reject(err);
      },
    });
  });
}

export function get<T>(url: string, data?: Record<string, unknown>): Promise<T> {
  return request<T>({ url, method: "GET", data });
}

export function post<T>(url: string, data?: Record<string, unknown>): Promise<T> {
  return request<T>({ url, method: "POST", data });
}

export function put<T>(url: string, data?: Record<string, unknown>): Promise<T> {
  return request<T>({ url, method: "PUT", data });
}

export function del<T>(url: string): Promise<T> {
  return request<T>({ url, method: "DELETE" });
}
