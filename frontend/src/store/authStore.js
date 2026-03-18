import { create } from 'zustand'

const useAuthStore = create((set) => ({
  user: null,
  authHydrated: false,
  setUser: (user) =>
    set({
      user,
      authHydrated: true,
    }),
  hydrateAuth: (session) =>
    set({
      user: session?.user ?? null,
      authHydrated: true,
    }),
  clearUser: () =>
    set({
      user: null,
      authHydrated: true,
    }),
}))

export default useAuthStore
