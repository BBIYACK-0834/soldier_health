const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '');
const UPLOADS_PATH_PREFIX = '/uploads/profile-images/';

function isLocalhost(hostname) {
  return hostname === 'localhost' || hostname === '127.0.0.1' || hostname === '0.0.0.0';
}

export function getProfileImageSrc(profileImageUrl) {
  if (!profileImageUrl) {
    return '';
  }

  if (profileImageUrl.startsWith(UPLOADS_PATH_PREFIX)) {
    return `${API_BASE_URL}${profileImageUrl}`;
  }

  try {
    const parsedUrl = new URL(profileImageUrl);
    if (parsedUrl.pathname.startsWith(UPLOADS_PATH_PREFIX) && isLocalhost(parsedUrl.hostname)) {
      return `${API_BASE_URL}${parsedUrl.pathname}${parsedUrl.search}${parsedUrl.hash}`;
    }
  } catch {
    // Non-URL values are returned as-is so existing preset/data URL/object URL previews keep working.
  }

  return profileImageUrl;
}
