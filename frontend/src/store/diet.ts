import { defineStore } from "pinia";
import { ref } from "vue";
import type { DailyRecipe } from "@/api/recipe";
import type { DietRecord, RecordSummary } from "@/api/record";

export const useDietStore = defineStore("diet", () => {
  const todayRecipe = ref<DailyRecipe | null>(null);
  const todayRecords = ref<DietRecord[]>([]);
  const todaySummary = ref<RecordSummary | null>(null);

  function setRecipe(recipe: DailyRecipe): void {
    todayRecipe.value = recipe;
  }

  function setRecords(records: DietRecord[]): void {
    todayRecords.value = records;
  }

  function setSummary(summary: RecordSummary): void {
    todaySummary.value = summary;
  }

  function clear(): void {
    todayRecipe.value = null;
    todayRecords.value = [];
    todaySummary.value = null;
  }

  return { todayRecipe, todayRecords, todaySummary, setRecipe, setRecords, setSummary, clear };
});
