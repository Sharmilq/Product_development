/**
 * Java-compatible hash code function.
 * Produces the same integer user_id as the Android app's hashCode()
 * method, which allows web and mobile app to share the same database rows.
 */
export function getJavaHashCode(str) {
  let hash = 0
  for (let i = 0; i < str.length; i++) {
    hash = (31 * hash + str.charCodeAt(i)) | 0
  }
  return Math.abs(hash)
}
