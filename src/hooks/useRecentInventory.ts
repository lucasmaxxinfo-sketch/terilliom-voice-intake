import { useQuery } from "@tanstack/react-query";

import { inventoryRepository } from "@/lib/repositories/inventory.repository";

export function useRecentInventory(limit = 10) {
  return useQuery({
    queryKey: ["inventory", "recent", limit],
    queryFn: () => inventoryRepository.listRecent(limit),
  });
}

export function useTodayIntakeCount() {
  return useQuery({
    queryKey: ["inventory", "today-count"],
    queryFn: () => {
      const start = new Date();
      start.setHours(0, 0, 0, 0);
      return inventoryRepository.countCreatedSince(start.toISOString());
    },
  });
}
