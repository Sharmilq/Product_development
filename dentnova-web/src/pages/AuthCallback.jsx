import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { supabase } from "../lib/supabase";

export default function AuthCallback() {
  const navigate = useNavigate();

  useEffect(() => {
    async function handleCallback() {
      console.log("AUTH_CALLBACK_HIT", window.location.href);

      const { data, error } = await supabase.auth.getSession();

      console.log("AUTH_CALLBACK_SESSION", data?.session);
      console.log("AUTH_CALLBACK_ERROR", error);

      if (error) {
        navigate("/auth?error=google_login_failed");
        return;
      }

      if (data?.session) {
        navigate("/dashboard", { replace: true });
      } else {
        setTimeout(async () => {
          const { data: retryData } = await supabase.auth.getSession();
          if (retryData?.session) {
            navigate("/dashboard", { replace: true });
          } else {
            navigate("/auth?error=no_session_found");
          }
        }, 1000);
      }
    }

    handleCallback();
  }, [navigate]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100 transition-colors">
      <div className="text-center">
        <div className="w-12 h-12 border-4 border-cyan-500/30 border-t-cyan-500 rounded-full animate-spin mx-auto mb-4" />
        <p className="text-lg font-semibold">Signing you in...</p>
        <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">Completing Google Sign-In, please wait.</p>
      </div>
    </div>
  );
}

