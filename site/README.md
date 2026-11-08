# Static marketing site

This directory is a self-contained static output root. It contains HTML, CSS, and a JSON intended-origin manifest; it does not include or serve the local unwrap backend.

## Local preview

From the repository root, run:

    python3 -m http.server 8091 --directory site

Open http://127.0.0.1:8091/. Stop only this temporary static-preview process after testing. Do not stop the existing local tool backend if one is running on 127.0.0.1:8080.

## GitHub Pages

- Public URL: https://iqdoctor.github.io/oracle-plsql-unwrapper/.
- `.github/workflows/publish-static-pages.yml` deploys only this `site/` directory through GitHub Actions; no framework build step is required.
- The repository's Pages publishing source must be set to **GitHub Actions**.
- This is a project site on the `github.io` domain. No custom domain, Cloudflare, backend, or DNS configuration is used.
- Do not place `tool/backend/`, secrets, submitted source, or runtime data in this directory.

## Pages

- index.html — overview and resource hub
- tool.html — static paste/upload and result-panel preview; no backend is connected
- article.html — long-form article
- presentation.html — ten-slide static presentation with visible speaker notes
- assets/site.css — shared responsive, keyboard-focus, and print styles
