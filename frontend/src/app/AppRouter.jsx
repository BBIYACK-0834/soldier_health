import { Navigate, Route, Routes } from 'react-router-dom';
import { ACCESS_TOKEN_KEY } from '../api/httpClient';
import OnboardingPage from '../features/onboarding/OnboardingPage';
import LoginPage from '../features/auth/LoginPage';
import SignupPage from '../features/auth/SignupPage';
import UnitSelectPage from '../features/setup/UnitSelectPage';
import UnitCompletePage from '../features/setup/UnitCompletePage';
import GuidePage from '../features/guide/GuidePage';
import EquipmentSelectPage from '../features/setup/EquipmentSelectPage';
import ProfileSetupPage from '../features/setup/ProfileSetupPage';
import HomePage from '../features/home/HomePage';
import NutritionPage from '../features/nutrition/NutritionPage';
import DietAddPage from '../features/nutrition/DietAddPage';
import WorkoutPage from '../features/workout/WorkoutPage';
import WorkoutEditPage from '../features/workout/WorkoutEditPage';
import CommunityPage from '../features/community/CommunityPage';
import ProfilePage from '../features/profile/ProfilePage';
import MyPostsPage from '../features/profile/MyPostsPage';
import GoalSettingsPage from '../features/profile/GoalSettingsPage';
import NotificationSettingsPage from '../features/profile/NotificationSettingsPage';
import DataManagementPage from '../features/profile/DataManagementPage';

function RequireAuth({ children }) {
  const token = localStorage.getItem(ACCESS_TOKEN_KEY);

  if (!token) {
    return <Navigate to="/login" replace />;
  }

  return children;
}

export default function AppRouter() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/onboarding" replace />} />
      <Route path="/onboarding" element={<OnboardingPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />

      <Route path="/unit/setup" element={<RequireAuth><UnitSelectPage /></RequireAuth>} />
      <Route path="/unit/search" element={<RequireAuth><UnitSelectPage /></RequireAuth>} />
      <Route path="/unit/select/:unitId" element={<RequireAuth><UnitSelectPage /></RequireAuth>} />
      <Route path="/unit/complete" element={<RequireAuth><UnitCompletePage /></RequireAuth>} />
      <Route path="/guide" element={<RequireAuth><GuidePage /></RequireAuth>} />

      <Route path="/setup/unit" element={<Navigate to="/unit/setup" replace />} />
      <Route path="/setup/equipment" element={<RequireAuth><EquipmentSelectPage /></RequireAuth>} />
      <Route path="/setup/profile" element={<RequireAuth><ProfileSetupPage /></RequireAuth>} />

      <Route path="/home" element={<RequireAuth><HomePage /></RequireAuth>} />
      <Route path="/diet" element={<RequireAuth><NutritionPage /></RequireAuth>} />
      <Route path="/diet/add" element={<RequireAuth><DietAddPage /></RequireAuth>} />
      <Route path="/diet/search" element={<RequireAuth><DietAddPage /></RequireAuth>} />
      <Route path="/diet/:date" element={<RequireAuth><NutritionPage /></RequireAuth>} />

      <Route path="/exercise" element={<RequireAuth><WorkoutPage /></RequireAuth>} />
      <Route path="/exercise/add/equipment" element={<RequireAuth><WorkoutEditPage /></RequireAuth>} />
      <Route path="/exercise/datasets" element={<RequireAuth><WorkoutEditPage /></RequireAuth>} />
      <Route path="/exercise/routine/edit" element={<RequireAuth><WorkoutEditPage /></RequireAuth>} />
      <Route path="/exercise/routine/:routineId/edit" element={<RequireAuth><WorkoutEditPage /></RequireAuth>} />
      <Route path="/workout" element={<Navigate to="/exercise" replace />} />
      <Route path="/workout/edit" element={<Navigate to="/exercise/routine/edit" replace />} />

      <Route path="/community" element={<RequireAuth><CommunityPage /></RequireAuth>} />
      <Route path="/community/popular" element={<RequireAuth><CommunityPage /></RequireAuth>} />
      <Route path="/community/unit" element={<RequireAuth><CommunityPage /></RequireAuth>} />
      <Route path="/community/posts/:postId" element={<RequireAuth><CommunityPage /></RequireAuth>} />

      <Route path="/mypage" element={<RequireAuth><ProfilePage /></RequireAuth>} />
      <Route path="/mypage/posts" element={<RequireAuth><MyPostsPage /></RequireAuth>} />
      <Route path="/mypage/goal" element={<RequireAuth><GoalSettingsPage /></RequireAuth>} />
      <Route path="/mypage/notifications" element={<RequireAuth><NotificationSettingsPage /></RequireAuth>} />
      <Route path="/mypage/data" element={<RequireAuth><DataManagementPage /></RequireAuth>} />

      <Route path="*" element={<Navigate to="/onboarding" replace />} />
    </Routes>
  );
}
