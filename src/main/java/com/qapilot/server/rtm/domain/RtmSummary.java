package com.qapilot.server.rtm.domain;

/**
 * RTM 요약 통계.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record RtmSummary(int total, int satisfied, int unsatisfied, int unmeasured) {

    public static RtmSummary from(java.util.List<RtmRequirement> requirements) {
        int satisfied = 0;
        int unsatisfied = 0;
        int unmeasured = 0;
        for (RtmRequirement req : requirements) {
            if ("충족".equals(req.status())) satisfied++;
            else if ("미충족".equals(req.status())) unsatisfied++;
            else unmeasured++;
        }
        return new RtmSummary(requirements.size(), satisfied, unsatisfied, unmeasured);
    }
}
