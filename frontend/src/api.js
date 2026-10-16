import axios from 'axios';

export const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

const api = axios.create({ baseURL: API_URL });

api.interceptors.request.use((cfg) => {
  const t = localStorage.getItem('token');
  if (t) cfg.headers.Authorization = `Bearer ${t}`;
  return cfg;
});

export const msgErreur = (e) =>
  e?.response?.data?.message || (e?.response ? `Erreur ${e.response.status}` : 'Serveur injoignable');

export const CENTRE = { lat: 12.5681, lon: -16.2719 }; // Ziguinchor

export default api;
