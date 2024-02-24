#
# Copyright 2024 Thoughtworks, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

Rails.application.routes.draw do
  # For details on the DSL available within this file, see https://guides.rubyonrails.org/routing.html

  unless defined?(CONSTANTS)
    GROUP_NAME_FORMAT = PIPELINE_NAME_FORMAT = STAGE_NAME_FORMAT = /[\w\-][\w\-.]*/
    PIPELINE_COUNTER_FORMAT = STAGE_COUNTER_FORMAT = /-?\d+/
    PIPELINE_LOCATOR_CONSTRAINTS = {:pipeline_name => PIPELINE_NAME_FORMAT, :pipeline_counter => PIPELINE_COUNTER_FORMAT}
    STAGE_LOCATOR_CONSTRAINTS = {:stage_name => STAGE_NAME_FORMAT, :stage_counter => STAGE_COUNTER_FORMAT}.merge(PIPELINE_LOCATOR_CONSTRAINTS)
    PLUGIN_ID_FORMAT = /[\w\-.]+/
    CONSTANTS = true
  end

  # This is used to generate _url and _path in application_helper#url_for_path
  get "/", to: redirect("/go/pipelines"), as: :root

  get "admin/pipelines/snippet" => "admin/pipelines_snippet#index", as: :pipelines_snippet
  get "admin/pipelines/snippet/:group_name" => "admin/pipelines_snippet#show", constraints: {group_name: GROUP_NAME_FORMAT}, as: :pipelines_snippet_show
  get "admin/pipelines/snippet/:group_name/edit" => "admin/pipelines_snippet#edit", constraints: {group_name: GROUP_NAME_FORMAT}, as: :pipelines_snippet_edit
  put "admin/pipelines/snippet/:group_name" => "admin/pipelines_snippet#update", constraints: {group_name: GROUP_NAME_FORMAT}, as: :pipelines_snippet_update
  get "admin/config_change/between/:later_md5/and/:earlier_md5" => "admin/stages#config_change", as: :admin_config_change

  get "admin/config_xml" => "admin/configuration#show", as: :config_view
  put "admin/config_xml" => "admin/configuration#update", as: :config_update
  get "admin/config_xml/edit" => "admin/configuration#edit", as: :config_edit

  #The analytics routes are used in javascript
  resources :analytics, only: [:index], controller: "analytics"
  get "analytics/:plugin_id/:type/:id" => "analytics#show", constraints: {plugin_id: PLUGIN_ID_FORMAT, id: PIPELINE_NAME_FORMAT}, as: :show_analytics

  get "pipelines/value_stream_map/:pipeline_name/:pipeline_counter(.:format)" => "value_stream_map#show", constraints: {:pipeline_name => PIPELINE_NAME_FORMAT, :pipeline_counter => PIPELINE_COUNTER_FORMAT}, defaults: {:format => :html}, as: :vsm_show
  get "materials/value_stream_map/:material_fingerprint/:revision(.:format)" => "value_stream_map#show_material", defaults: {:format => :html}, constraints: {:revision => /[^\/]+(?=\.html\z|\.json\z)|[^\/]+/}, as: :vsm_show_material

  post "pipelines/:pipeline_name/:pipeline_counter/:stage_name/:stage_counter/rerun-jobs" => "stages#rerun_jobs", as: :rerun_jobs, constraints: STAGE_LOCATOR_CONSTRAINTS
  get "pipelines/:pipeline_name/:pipeline_counter/:stage_name/:stage_counter" => "stages#overview", as: "stage_detail_tab_default", constraints: STAGE_LOCATOR_CONSTRAINTS

  %w(overview pipeline materials jobs stats stats_iframe stage_config).each do |controller_action_method|
    get "pipelines/:pipeline_name/:pipeline_counter/:stage_name/:stage_counter/#{controller_action_method}" => "stages##{controller_action_method}", as: "stage_detail_tab_#{controller_action_method}", constraints: STAGE_LOCATOR_CONSTRAINTS
  end

  get "history/stage/:pipeline_name/:pipeline_counter/:stage_name/:stage_counter" => "stages#history", as: :stage_history, constraints: STAGE_LOCATOR_CONSTRAINTS

  scope "internal" do
    # redirects to first-stage details page of the specific pipeline run
    get "pipelines/:pipeline_name/:pipeline_counter" => "stages#redirect_to_first_stage", as: :internal_stage_detail_tab, constraints: PIPELINE_LOCATOR_CONSTRAINTS
  end
end
