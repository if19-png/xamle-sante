import { createContext, useContext, useState } from 'react';

const Ctx = createContext(null);

function lire() {
  const t = localStorage.getItem('token');
  if (!t) return null;
  try {
    const p = JSON.parse(atob(t.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
    if (p.exp * 1000 < Date.now()) return null;
    return { token: t, email: p.sub, role: p.role, uid: p.uid };
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(lire());
  const connecter = (token) => {
    localStorage.setItem('token', token);
    setUser(lire());
  };
  const deconnecter = () => {
    localStorage.removeItem('token');
    setUser(null);
  };
  return <Ctx.Provider value={{ user, connecter, deconnecter }}>{children}</Ctx.Provider>;
}

export const useAuth = () => useContext(Ctx);
