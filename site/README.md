# Static marketing site

This directory is a self-contained static output root. It contains HTML, CSS, and a JSON intended-origin manifest; it does not include or serve the local unwrap backend.

## Local preview

From the repository root, run:

    python3 -m http.server 8091 --directory site

Open http://127.0.0.1:8091/. Stop only this temporary static-preview process after testing. Do not stop the existing local tool backend if one is running on 127.0.0.1:8080.

## Future static hosting

- Configure the static host's output/root directory as site/.
- No framework build step is required; index.html is the root entry point.
- Intended public origin: https://unwrap.iqdoctor.pro.
- site-config.json is an informational intended-origin manifest only. It does not configure Cloudflare, DNS, certificates, or a custom-domain binding.
- Do not place tool/backend/, secrets, source submissions, or runtime data in this directory.
- Before publication, separately verify Cloudflare project setup, account access, custom-domain association, DNS, TLS, and the final public copy. None of those operations has been performed by this repository content.

## Pages

- index.html — overview and resource hub
- article.html — long-form article
- presentation.html — ten-slide static presentation with visible speaker notes
- assets/site.css — shared responsive, keyboard-focus, and print styles
