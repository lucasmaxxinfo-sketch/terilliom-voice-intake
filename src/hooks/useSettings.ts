import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import type { AppSettings } from "@/lib/models/setting";
import { settingsService } from "@/lib/services/settings/settings.service";

const KEY = ["settings"] as const;

export function useSettings() {
  return useQuery({
    queryKey: KEY,
    queryFn: () => settingsService.load(),
  });
}

export function useUpdateSettings() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (patch: Partial<AppSettings>) => settingsService.patch(patch),
    onSuccess: (next) => qc.setQueryData(KEY, next),
  });
}
