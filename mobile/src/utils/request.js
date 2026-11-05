// 统一请求封装：带 token、区分 401/403、统一抛错信息
// H5 走 vite 代理（相对路径）；小程序 / App 需要完整后端地址
// #ifdef H5
const BASE_URL = ''
// #endif
// #ifndef H5
const BASE_URL = 'http://localhost:8083'
// #endif

export function getToken() {
  return uni.getStorageSync('token') || ''
}

export function clearLogin() {
  uni.removeStorageSync('token')
  uni.removeStorageSync('userInfo')
}

export function request(options) {
  const token = getToken()
  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      timeout: 15000,
      header: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: token } : {})
      },
      success(res) {
        if (res.statusCode === 401) {
          clearLogin()
          uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
          setTimeout(() => {
            uni.reLaunch({ url: '/pages/login/login' })
          }, 800)
          reject(new Error('未登录或登录已过期'))
          return
        }
        if (res.statusCode === 403) {
          reject(new Error('没有访问权限'))
          return
        }
        const body = res.data || {}
        if (res.statusCode >= 200 && res.statusCode < 300 && body.code === 0) {
          resolve(body)
          return
        }
        reject(new Error(body.msg || body.message || '请求失败'))
      },
      fail(err) {
        reject(new Error(err.errMsg || '网络异常，请稍后重试'))
      }
    })
  })
}

export function get(url, data) {
  return request({ url, method: 'GET', data })
}

export function post(url, data) {
  return request({ url, method: 'POST', data })
}
