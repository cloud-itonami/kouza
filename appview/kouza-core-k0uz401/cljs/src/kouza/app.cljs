(ns kouza.app
  "kouza-core-k0uz401 appview — reagent + re-frame, view built from
  jp-go-dds (デジタル庁デザインシステム) hiccup.

  This is a faithful port of the previous SvelteKit scaffold
  (`svelte/src/routes/+page.svelte`): a static surface-info card (title,
  project, route/var counts, declared routes, declared vars, source path).
  It does not add account-aggregation functionality that page never had —
  this surface's declared capabilities (`connection-registry`,
  `statement-import`, …) are implemented elsewhere (see kotodama.jsonld /
  README); this scaffold only reports on the surface itself, exactly as the
  Svelte page did.

  `public/index.html` is a generated shell, not a hand-edited file. It is
  `jp-go-dds.page/->page` output — the vendored `dds.css` concatenated with
  `jp-go-dds.core/ext-css` — produced once at authoring time against the
  git/sha this deps.edn pins. Regenerate it (JVM-free, via nbb) from this
  directory whenever that pin moves:

      R=<superproject root>
      D=$R/orgs/kotoba-lang/jp-go-digital-design-system
      H=$R/orgs/kotoba-lang/html
      C=$R/orgs/kotoba-lang/css
      nbb --classpath \"$D/src:$D/resources:$H/src:$C/src\" -e '
      (ns g (:require [jp-go-dds.page :as page] [\"fs\" :as fs]))
      (def css (fs/readFileSync \"'\"$D\"'/resources/jp_go_dds/dds.css\" \"utf8\"))
      (fs/writeFileSync \"public/index.html\"
        (page/->page {:title \"kouza-core-k0uz401\" :lang \"ja\"
                      :description \"Kouza Core K0uz401 — read-only financial account aggregator appview scaffold\"
                      :css css}
                     [:div {:id \"app\"} \"kouza-core-k0uz401 loading…\"]
                     [:noscript \"kouza-core-k0uz401 requires JavaScript.\"]
                     [:script {:src \"js/app.js\"}]))'

  The `:noscript` child is part of the shell and is easy to lose: `->page`
  does not add one, so a regeneration that omits it drops the no-JavaScript
  fallback silently and the page still looks correct in a browser."
  (:require [reagent.dom :as rdom]
            [re-frame.core :as rf]
            [jp-go-dds.core :as dds]))

;; -- db ------------------------------------------------------------------
;;
;; The Svelte scaffold held this as a static, unwired object literal inside
;; `<script>` — never read from the environment or from wrangler.jsonc. The
;; migration keeps every value identical (routeCount 0, routes [], vars [])
;; and updates two fields it would otherwise make untrue:
;;
;; - `:relative-path` names where *this* file lives, and the file moved.
;; - `:xrpc?` is now false. The Svelte page declared `xrpc: true`, and that
;;   was accurate while wrangler.jsonc ran a SvelteKit worker containing
;;   `svelte/src/routes/xrpc/[...path]/+server.ts`. This migration drops the
;;   `main` key entirely (no Worker source here calls `env.ASSETS.fetch`),
;;   so only static assets are served and `not_found_handling` is "none" —
;;   `/xrpc/*` now 404s. The handler itself was preserved, not deleted, at
;;   `../src/xrpc-mcp-router-proxy.ts`; whether to rewire it is an open
;;   product decision, not one this migration made. Reporting "enabled" on
;;   a page whose only job is to describe this surface would be a lie.
;;
;; Held in the re-frame db + subs so
;; the migration exercises the event/sub plumbing the workspace standard
;; calls for, even though the source data itself is still static.

(def default-db
  {:title "Kouza Core K0uz401"
   :project "etzhayyim-project-kouza"
   :name "kouza-core-k0uz401"
   :kind "appview"
   :route-count 0
   :routes []
   :vars []
   :xrpc? false
   :relative-path "appview/kouza-core-k0uz401/cljs/src/kouza/app.cljs"})

(rf/reg-event-db
 :initialize-db
 (fn [_ _] default-db))

(rf/reg-sub :title (fn [db _] (:title db)))
(rf/reg-sub :project (fn [db _] (:project db)))
(rf/reg-sub :name (fn [db _] (:name db)))
(rf/reg-sub :kind (fn [db _] (:kind db)))
(rf/reg-sub :route-count (fn [db _] (:route-count db)))
(rf/reg-sub :routes (fn [db _] (:routes db)))
(rf/reg-sub :vars (fn [db _] (:vars db)))
(rf/reg-sub :xrpc? (fn [db _] (:xrpc? db)))
(rf/reg-sub :relative-path (fn [db _] (:relative-path db)))

;; -- view ------------------------------------------------------------------

(defn- top-section [title kind name]
  (dds/section {}
    [:p {:class "dds-ext-lead"} (str "Cloudflare " kind)]
    (dds/heading 1 title)
    (dds/chip-label name {:color "gray"})))

(defn- facts-section [project route-count xrpc?]
  (dds/grid {}
    (dds/card [:p {:class "dds-ext-lead"} "Project"] [:strong project])
    (dds/card [:p {:class "dds-ext-lead"} "Routes"] [:strong (str route-count)])
    (dds/card [:p {:class "dds-ext-lead"} "XRPC"]
              [:strong (if xrpc? "enabled" "not configured")])))

(defn- routes-section [routes]
  (dds/section {:title "Public Routes"}
    (if (seq routes)
      (dds/table {:headers ["Route"] :rows (map vector routes)})
      [:p {:class "dds-ext-lead"}
       "No public route is declared next to this app surface."])))

(defn- vars-section [vars]
  (dds/section {:title "Runtime Bindings"}
    (if (seq vars)
      (apply dds/row (map #(dds/chip-label %) vars))
      [:p {:class "dds-ext-lead"}
       "No public vars are declared in the nearest wrangler config."])))

(defn- source-section [relative-path]
  (dds/section {:title "Source"}
    [:p relative-path]))

(defn app-view []
  (let [title (rf/subscribe [:title])
        project (rf/subscribe [:project])
        name (rf/subscribe [:name])
        kind (rf/subscribe [:kind])
        route-count (rf/subscribe [:route-count])
        routes (rf/subscribe [:routes])
        vars (rf/subscribe [:vars])
        xrpc? (rf/subscribe [:xrpc?])
        relative-path (rf/subscribe [:relative-path])]
    (dds/container
     (top-section @title @kind @name)
     (facts-section @project @route-count @xrpc?)
     (routes-section @routes)
     (vars-section @vars)
     (source-section @relative-path))))

;; -- init --------------------------------------------------------------------

(defn ^:export main []
  (rf/dispatch-sync [:initialize-db])
  (rdom/render [app-view] (js/document.getElementById "app")))
