/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.sonatype.buildserver.eclipse.ui;

import org.eclipse.osgi.util.NLS;

public class Messages
    extends NLS
{
    private static final String BUNDLE = "com.sonatype.buildserver.eclipse.ui.messages"; //$NON-NLS-1$

    static
    {
        NLS.initializeMessages( BUNDLE, Messages.class );
    }

    public static String jobSubscriptionDialog_availableJobs;

    public static String jobSubscriptionDialog_buildServer;

    public static String jobSubscriptionDialog_clickLoadNow;

    public static String jobSubscriptionDialog_deselectAll;

    public static String jobSubscriptionDialog_empty_noJobsAvailable;

    public static String jobSubscriptionDialog_empty_noMatchingJobs;

    public static String jobSubscriptionDialog_empty_noServerSelected;

    public static String jobSubscriptionDialog_filterText;

    public static String jobSubscriptionDialog_labelTemplate;

    public static String jobSubscriptionDialog_loadJobs;

    public static String jobSubscriptionDialog_selectAll;

    public static String jobSubscriptionDialog_selectJobs;

    public static String jobSubscriptionDialog_title;

    public static String jobView_actions_add_title;

    public static String jobView_actions_add_tooltip;

    public static String jobView_actions_build_title;

    public static String jobView_actions_build_tooltip;

    public static String jobView_actions_copyServerUrl_title;

    public static String jobView_actions_copyServerUrl_tooltip;

    public static String jobView_actions_copyUrl_title;

    public static String jobView_actions_copyUrl_tooltip;

    public static String jobView_actions_open_title;

    public static String jobView_actions_open_tooltip;

    public static String jobView_actions_refresh_title;

    public static String jobView_actions_refresh_tooltip;

    public static String jobView_actions_remove_title;

    public static String jobView_actions_remove_tooltip;

    public static String jobView_actions_show_test_results_title;

    public static String jobView_actions_show_test_results_tooltip;
    
    public static String jobView_actions_show_console_title;

    public static String jobView_actions_show_console_tooltip;
    
    public static String jobView_columns_lastBuild;

    public static String jobView_columns_name;

    public static String jobView_columns_server;

    public static String jobView_columns_status;

    public static String jobView_columns_weather;

    public static String jobPopup_view_build_results;

    public static String jobPopup_hour_ago;

    public static String jobPopup_hours_ago;

    public static String jobPopup_day_ago;

    public static String jobPopup_days_ago;

    public static String jobPopup_week_ago;

    public static String jobPopup_weeks_ago;

    public static String jobPopup_month_ago;

    public static String jobPopup_months_ago;

    public static String jobPopup_minutes_ago;

    public static String jobPopup_a_minute_ago;

    public static String jobPopup_not_run;

    public static String jobPopup_title;
    
    public static String hudsonMonitor_build_error;

    public static String hudsonMonitor_unknown_build_error;

    public static String consoleView_getCompleteContent;

    public static String consoleView_getContent_job;

    public static String error_unknown_host;
    
    public static String error_unresolved_address;
    
    public static String error_no_route_to_host;

    public static String background_build_job;
    
    public static String background_retrieve_job;

    public static String background_retrieve_jobs;
    
    public static String background_retrieve_builds;

    public static String background_retrieve_changes;
    
    public static String background_retrieve_tests;
    
    public static String background_loading_prefs;

    public static String ChangeBuildPanel_participants;

    public static String ChangeBuildPanel_view_detail_browser;

    public static String ChangeEntryFilePanel_open_file;

    public static String ChangeEntryPanel_author;

    public static String HudsonUtils_all_fail;

    public static String HudsonUtils_build_prefix;

    public static String HudsonUtils_error_open_url;

    public static String HudsonUtils_hour_minute;

    public static String HudsonUtils_majority_fail;

    public static String HudsonUtils_minority_fail;

    public static String HudsonUtils_minute_second;

    public static String HudsonUtils_no_recent_fail;

    public static String HudsonUtils_second;

    public static String HudsonUtils_some_fail;

    public static String HudsonUtils_test_name_prefix;

    public static String HudsonUtils_unknown_health;

    public static String JobSubscriptionDialog_filter_hint;

    public static String JobView_hide_build;

    public static String JobView_hide_server;

    public static String JobView_no_monitored_job_link;

    public static String JobView_show_build;

    public static String JobView_show_server;

    public static String JobView_workspace_browser;

    public static String ScmChangesPage_build_changes_action;

    public static String ScmChangesPage_build_prefix;

    public static String ScmChangesPage_loading;

    public static String ScmChangesPage_open_action;

    public static String ScmChangesPage_open_all_action;

    public static String ScmChangesPage_recent_changes;

    public static String ScmChangesPage_section_title;

    public static String ScmChangesPage_show_recent_action;

    public static String ScmChangesPage_title;

    public static String SingleJobView_tooltip;

    public static String SummaryPage_aborted;

    public static String SummaryPage_blocked;

    public static String SummaryPage_build_link;

    public static String SummaryPage_build_link_tooptip;

    public static String SummaryPage_build_on;

    public static String SummaryPage_build_on_tooltip;

    public static String SummaryPage_build_state_unknown;

    public static String SummaryPage_changes_by;

    public static String SummaryPage_completed;

    public static String SummaryPage_configuration;

    public static String SummaryPage_console;

    public static String SummaryPage_console_tooktip;

    public static String SummaryPage_disabled;

    public static String SummaryPage_duration;

    public static String SummaryPage_enabled;

    public static String SummaryPage_external;

    public static String SummaryPage_failed;

    public static String SummaryPage_freestyle;

    public static String SummaryPage_job_link;

    public static String SummaryPage_job_tooltip;

    public static String SummaryPage_matrix;

    public static String SummaryPage_no_builds_yet;

    public static String SummaryPage_no_description;

    public static String SummaryPage_queued;

    public static String SummaryPage_running;

    public static String SummaryPage_section_build;

    public static String SummaryPage_section_job;

    public static String SummaryPage_started;

    public static String SummaryPage_test_results;

    public static String SummaryPage_test_results_tooltip;

    public static String SummaryPage_title;

    public static String SummaryPage_unknown;

    public static String SummaryPage_unstable;

    public static String SummaryPage_workspace;

    public static String SummaryPage_workspace_tooltip;
}
