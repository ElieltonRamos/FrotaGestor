export const API_URL = 'http://10.1.254.19:3001';
// export const API_URL = 'http://localhost:3001';

export function mapNetworkError(
  err: any,
  fallback = 'Erro inesperado'
): string {
  if (err.status === 0 || err.message?.includes('ERR_CONNECTION_REFUSED')) {
    return 'Não foi possível conectar ao servidor. Tente novamente mais tarde.';
  }
  return fallback;
}
