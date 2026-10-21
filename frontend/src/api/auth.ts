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

export const authApi = { wxLogin };

