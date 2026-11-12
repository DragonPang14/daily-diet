import { post } from "./http";

export interface WxLoginRequest {
  code: string;
}

export interface LoginResponse {
  token: string;
  isNewUser: boolean;
}

/** 微信 code 换 JWT */
export function wxLogin(code: string): Promise<LoginResponse> {
  return post<LoginResponse>("/auth/wx-login", { code });
}

/** 开发环境登录：后端直接签发测试用户 JWT（仅后端 dev profile 提供） */
export function devLogin(): Promise<LoginResponse> {
  return post<LoginResponse>("/auth/dev-login");
}

export const authApi = { wxLogin, devLogin };

