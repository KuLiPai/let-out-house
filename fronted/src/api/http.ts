import axios from "axios";

const http = axios.create({ baseURL: "/", timeout: 15000 });

http.interceptors.request.use((cfg) => {
  const token = localStorage.getItem("token");
  if (token) cfg.headers.Authorization = `Bearer ${token}`;
  return cfg;
});

http.interceptors.response.use(
  (res) => {
    const body = res.data;
    if (body.code !== 0)
      return Promise.reject(new Error(body.msg || "请求失败"));
    return body.data;
  },
  (err) => {
    const status = err.response?.status;
    const msg = err.response?.data?.msg || err.message;
    if (status === 401) {
      localStorage.removeItem("token");
      localStorage.removeItem("isLogin");
      if (location.pathname !== "/login") location.href = "/login";
    }
    return Promise.reject(new Error(msg));
  },
);

export default http;
