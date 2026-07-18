# Deploying RoamSafe on Oracle Cloud "Always Free"

A $0/month, always-on deployment: an Oracle Always Free ARM VM running the app
in Docker, with Caddy for automatic HTTPS. The database stays on Neon.

The app is 12-factor: every secret comes from an env var, so nothing sensitive
lives in the image or the repo.

---

## What you'll end up with

```
Internet ──HTTPS──▶ Caddy (:443) ──▶ RoamSafe container (:8080) ──▶ Neon Postgres
                        (auto Let's Encrypt cert)
```

Prerequisites: an Oracle Cloud account (free; a card is required for identity
verification but Always Free resources never bill), and a domain **or** a free
DuckDNS subdomain for HTTPS (Bachs webhooks require HTTPS).

---

## Part A — Create the Always Free VM

1. Oracle Cloud console → **Compute → Instances → Create instance**.
2. **Image:** Ubuntu 22.04.
3. **Shape:** change to **Ampere / VM.Standard.A1.Flex** (this is the Always
   Free ARM shape). Give it **1 OCPU / 6 GB** (all within the free allowance;
   you can go up to 4 OCPU / 24 GB free).
4. **SSH keys:** upload your public key (or let it generate one and download the
   private key).
5. Create. Note the **public IP**.

> Our Docker images are multi-arch, so ARM (Ampere) is fine.

---

## Part B — Open the network (the classic Oracle gotcha)

Oracle blocks traffic in **two** places. Open **both** for ports 80 and 443.

**1. Cloud Security List:** VM's subnet → **Security Lists** → default list →
**Add Ingress Rules**: source `0.0.0.0/0`, TCP, destination ports `80` then
`443`.

**2. The OS firewall** (SSH in first: `ssh ubuntu@YOUR_IP`):

```bash
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 80 -j ACCEPT
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 443 -j ACCEPT
sudo netfilter-persistent save
```

---

## Part C — Install Docker

```bash
sudo apt-get update && sudo apt-get install -y docker.io git
sudo systemctl enable --now docker
sudo usermod -aG docker ubuntu    # then log out and back in
```

---

## Part D — Deploy the app

```bash
git clone https://github.com/<you>/Globe-Trotter.git roamsafe
cd roamsafe
```

Create the production env file (**never commit this**):

```bash
nano prod.env
```

```dotenv
# --- Database (keep Neon) ---
DB_URL=jdbc:postgresql://YOUR-neon-host/neondb?sslmode=require
DB_USER=...
DB_PASS=...
DB_DRIVER=org.postgresql.Driver

# --- AI ---
GEMINI_API_KEY=...

# --- Bachs: LIVE for production ---
# Do NOT set BACH_DEMO_KEY in prod (that would force sandbox).
BACH_API_KEY=sk_live_...
BACHS_TRIP_PASS_PRODUCT=prod_fe6bdaeda81e44f381d3
BACHS_NOMAD_PRODUCT=prod_e32c1dd954364b7a96a2
BACHS_WEBHOOK_SECRET=...            # from the live webhook endpoint you create

# --- Optional ---
GOOGLE_SHEETS_URL=...
```

Build and run (the container defaults to the `prod` profile and restarts on
reboot/crash):

```bash
docker build -t roamsafe .
docker run -d --name roamsafe --restart unless-stopped \
  --env-file prod.env -p 127.0.0.1:8080:8080 roamsafe
```

Check it's up: `docker logs -f roamsafe` → wait for "Started RoamSafeApplication".
`curl -s localhost:8080/ | head` should return HTML.

> We bind to `127.0.0.1:8080` so the app is only reachable through Caddy, never
> directly on the public IP.

---

## Part E — HTTPS with Caddy

Point your domain (or a free `roamsafe.duckdns.org`) at the VM's public IP with
an **A record**. Then:

```bash
sudo apt-get install -y debian-keyring debian-archive-keyring apt-transport-https curl
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/gpg.key' | sudo gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt' | sudo tee /etc/apt/sources.list.d/caddy-stable.list
sudo apt-get update && sudo apt-get install -y caddy
```

Edit `/etc/caddy/Caddyfile` to just:

```
your-domain.com {
    reverse_proxy 127.0.0.1:8080
}
```

```bash
sudo systemctl restart caddy
```

Caddy fetches a Let's Encrypt certificate automatically. Visit
`https://your-domain.com` — RoamSafe over HTTPS.

---

## Part F — Point Bachs at it & go live

1. In the Bachs dashboard (**live mode**), create a webhook endpoint:
   `https://your-domain.com/api/bachs/webhook`, events: Customer Subscription
   Created / Updated / Deleted, Invoice Paid, Collection Succeeded.
2. Copy its signing secret into `prod.env` as `BACHS_WEBHOOK_SECRET`, then
   `docker restart roamsafe`.
3. Confirm live products exist and their ids match `BACHS_*_PRODUCT`.

Go-live checklist:
- [ ] `BACH_DEMO_KEY` is **absent** in prod.env (so it runs live, not sandbox)
- [ ] Live Bachs webhook secret set
- [ ] A real $3 Trip Pass purchase flips the account to Pro
- [ ] (Recommended) Restore proper CSRF — see the TODO in `SecurityConfig`

---

## Redeploying after a code change

```bash
cd roamsafe && git pull
docker build -t roamsafe .
docker rm -f roamsafe
docker run -d --name roamsafe --restart unless-stopped \
  --env-file prod.env -p 127.0.0.1:8080:8080 roamsafe
```

## Troubleshooting

- **Can't reach the site:** re-check *both* firewalls (Part B). This is the #1
  Oracle issue.
- **Container exits immediately:** `docker logs roamsafe` — usually a bad
  `DB_URL` or missing env var.
- **Out of memory on a 1-OCPU VM:** the Dockerfile already caps the heap
  (`MaxRAMPercentage=70`); a 6 GB VM has plenty of headroom.
- **Webhook not granting Pro:** confirm the endpoint URL, that the secret
  matches, and watch `docker logs -f roamsafe` for `[bachs]` lines while testing.
