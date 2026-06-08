export const environment = {
  production: true,
  apiBaseUrl: '/api',                // Same-origin in production — served behind nginx
  appName: 'Sports Federation',
  appVersion: '1.0.0',

  tokenKey:        'fed_access_token',
  refreshTokenKey: 'fed_refresh_token',

  tokenRefreshBufferSeconds: 60,
};
