import { Navigate, Route, Routes } from 'react-router-dom';
import OnboardingPage from '../pages/OnboardingPage';
import LoginPage from '../pages/LoginPage';
import SignupPage from '../pages/SignupPage';
import HomePage from '../pages/HomePage';
import NutritionPage from '../pages/NutritionPage';
import WorkoutPage from '../pages/WorkoutPage';
import WorkoutEditPage from '../pages/WorkoutEditPage';
import CommunityPage from '../pages/CommunityPage';
import ProfilePage from '../pages/ProfilePage';

export default function AppRouter() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/onboarding" replace />} />
      <Route path="/onboarding" element={<OnboardingPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />
      <Route path="/home" element={<HomePage />} />
      <Route path="/diet" element={<NutritionPage />} />
      <Route path="/workout" element={<WorkoutPage />} />
      <Route path="/workout/edit" element={<WorkoutEditPage />} />
      <Route path="/community" element={<CommunityPage />} />
      <Route path="/mypage" element={<ProfilePage />} />
      <Route path="*" element={<Navigate to="/onboarding" replace />} />
    </Routes>
  );
}
