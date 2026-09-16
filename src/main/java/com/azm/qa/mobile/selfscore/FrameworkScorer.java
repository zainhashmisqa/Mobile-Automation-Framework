package com.azm.qa.mobile.selfscore;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.azm.qa.mobile.selfscore.criteria.CiCdIntegrationCriterion;
import com.azm.qa.mobile.selfscore.criteria.CodeStructureCriterion;
import com.azm.qa.mobile.selfscore.criteria.FlakinessCriterion;
import com.azm.qa.mobile.selfscore.criteria.LocatorStrategyCriterion;
import com.azm.qa.mobile.selfscore.criteria.ParallelismCriterion;
import com.azm.qa.mobile.selfscore.criteria.ReportingCompletenessCriterion;
import com.azm.qa.mobile.selfscore.criteria.ReportingInfrastructureCriterion;
import com.azm.qa.mobile.selfscore.criteria.ScreenObjectPatternCriterion;
import com.azm.qa.mobile.selfscore.criteria.SelfHealingAuthenticityCriterion;
import com.azm.qa.mobile.selfscore.criteria.TestPyramidCriterion;

/**
 * Scores a framework repo against the mobile-mapped 10-criteria rubric (adapted from the
 * web-framework assessment deck that scored this user's own web repo 8.4/10, with AI/Healing
 * at 6/10). Every criterion is a pure, read-only file-system scan -- the target repo is never
 * compiled or executed, so this can score a repo it did not build.
 */
public final class FrameworkScorer {

    /** Fixed order: matches the 10 web-deck axes this rubric was mapped from. */
    public static final List<Criterion> CRITERIA = List.of(
            new CodeStructureCriterion(),
            new ScreenObjectPatternCriterion(),
            new SelfHealingAuthenticityCriterion(),
            new ReportingInfrastructureCriterion(),
            new ReportingCompletenessCriterion(),
            new LocatorStrategyCriterion(),
            new CiCdIntegrationCriterion(),
            new FlakinessCriterion(),
            new ParallelismCriterion(),
            new TestPyramidCriterion());

    public ScoreReport evaluate(Path repoRoot, String label) {
        Map<Criterion, CriterionScore> results = new LinkedHashMap<>();
        for (Criterion criterion : CRITERIA) {
            CriterionScore score;
            try {
                score = criterion.evaluate(repoRoot);
            } catch (Exception e) {
                // A criterion that blows up on a malformed/unusual repo shouldn't take the
                // whole scorecard down with it -- score it 0 with the failure as evidence.
                score = CriterionScore.of(0, "Criterion threw an exception -- treated as unscoreable.",
                        e.getClass().getSimpleName() + ": " + e.getMessage());
            }
            results.put(criterion, score);
        }
        return ScoreReport.of(label, results);
    }
}
