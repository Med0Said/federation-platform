export const environment = {
  production: false,
  apiBaseUrl: '/api',                // proxied to http://localhost:8080/api
  appName: 'Sports Federation',
  appVersion: '1.0.0',

  // Token storage keys
  tokenKey:        'fed_access_token',
  refreshTokenKey: 'fed_refresh_token',

  // How many seconds before expiry to proactively refresh the token
  tokenRefreshBufferSeconds: 60,
};
