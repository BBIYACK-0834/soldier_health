import { Navigate, Route, Routes } from 'react-router-dom';
import { ACCESS_TOKEN_KEY } from '../api/httpClient';
import OnboardingPage from '../pages/OnboardingPage';
import LoginPage from '../pages/LoginPage';
import SignupPage from '../pages/SignupPage';
import UnitSelectPage from '../pages/UnitSelectPage';
import EquipmentSelectPage from '../pages/EquipmentSelectPage';
import ProfileSetupPage from '../pages/ProfileSetupPage';
import HomePage from '../pages/HomePage';
import NutritionPage from '../pages/NutritionPage';
import WorkoutPage from '../pages/WorkoutPage';
import WorkoutEditPage from '../pages/WorkoutEditPage';
import CommunityPage from '../pages/CommunityPage';
import ProfilePage from '../pages/ProfilePage';

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

      <Route
        path="/setup/unit"
        element={
          <RequireAuth>
            <UnitSelectPage />
          </RequireAuth>
        }
      />

      <Route
        path="/setup/equipment"
        element={
          <RequireAuth>
            <EquipmentSelectPage />
          </RequireAuth>
        }
      />

      <Route
        path="/setup/profile"
        element={
          <RequireAuth>
            <ProfileSetupPage />
          </RequireAuth>
        }
      />

      <Route
        path="/home"
        element={
          <RequireAuth>
            <HomePage />
          </RequireAuth>
        }
      />

      <Route
        path="/diet"
        element={
          <RequireAuth>
            <NutritionPage />
          </RequireAuth>
        }
      />

      <Route
        path="/workout"
        element={
          <RequireAuth>
            <WorkoutPage />
          </RequireAuth>
        }
      />

      <Route
        path="/workout/edit"
        element={
          <RequireAuth>
            <WorkoutEditPage />
          </RequireAuth>
        }
      />

      <Route
        path="/community"
        element={
          <RequireAuth>
            <CommunityPage />
          </RequireAuth>
        }
      />

      <Route
        path="/mypage"
        element={
          <RequireAuth>
            <ProfilePage />
          </RequireAuth>
        }
      />

      <Route path="*" element={<Navigate to="/onboarding" replace />} />
    </Routes>
  );
}