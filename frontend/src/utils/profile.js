export function isProfileReady(profile, hasUnit) {
  if (!profile) return false;

  const hasBody = Number(profile.heightCm) > 0 && Number(profile.weightKg) > 0;
  const hasWorkoutPrefs =
    Number(profile.workoutDaysPerWeek) >= 1 && Number(profile.preferredWorkoutMinutes) >= 10;

  return hasBody && hasWorkoutPrefs && Boolean(profile.goalType) && Boolean(profile.workoutLevel) && hasUnit;
}
