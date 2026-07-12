package middleware

import (
	"net/http"
	"strings"
	"time"
)

type TokenValidator interface {
	Validate(token string) (string, bool)
}

func AuthMiddleware(validator TokenValidator) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			authHeader := r.Header.Get("Authorization")
			if authHeader == "" || !strings.HasPrefix(authHeader, "Bearer ") {
				http.Error(w, `{"error":"missing token"}`, http.StatusUnauthorized)
				return
			}

			token := strings.TrimPrefix(authHeader, "Bearer ")
			userID, ok := validator.Validate(token)
			if !ok {
				http.Error(w, `{"error":"invalid or expired token"}`, http.StatusUnauthorized)
				return
			}

			r.Header.Set("X-User-ID", userID)
			r.Header.Set("X-Auth-Time", time.Now().Format(time.RFC3339))
			next.ServeHTTP(w, r)
		})
	}
}
