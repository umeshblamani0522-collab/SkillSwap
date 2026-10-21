# SkillSwap JaaS Video Setup

SkillSwap uses Jitsi as a Service (JaaS) rather than the public meet.jit.si deployment. The backend authorizes the SkillSwap participant, signs a short-lived JaaS JWT with the private key, and returns only the room metadata + JWT needed by the frontend. JaaS uses the 8x8.vc infrastructure.

Backend environment variables:

- `JAAS_ENABLED=true`
- `JAAS_APP_ID=<your JaaS AppID>`
- `JAAS_API_KEY_ID=<your API key id or full kid>`
- `JAAS_PRIVATE_KEY_PATH=<path to the PKCS#8 RSA private key PEM>`
- `JAAS_JWT_TTL_MINUTES=15`
- `JAAS_USE_STAGING=false`

Never place the private key in the React frontend or commit it to Git.
