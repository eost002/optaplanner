package org.acme.maintenancescheduling.solver;

import java.io.Console;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.swing.GroupLayout.Group;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.optaplanner.core.api.score.stream.ConstraintCollectors.sum;
import static org.optaplanner.core.api.score.stream.ConstraintCollectors.sumDuration;
import static org.optaplanner.core.api.score.stream.Joiners.equal;
import static org.optaplanner.core.api.score.stream.Joiners.filtering;
import static org.optaplanner.core.api.score.stream.Joiners.lessThan;
import static org.optaplanner.core.api.score.stream.Joiners.overlapping;

import org.acme.maintenancescheduling.domain.Job;
import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintCollectors;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;

public class MaintenanceScheduleConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[] {
                // Hard constraints
                crewConflict(constraintFactory),
                oddWeekTeamInRespectiveShiftA(constraintFactory),
                oddWeekTeamInRespectiveShiftB(constraintFactory),
                evenWeekTeamInRespectiveShiftA(constraintFactory),
                evenWeekTeamInRespectiveShiftB(constraintFactory),
                
                wednesday(constraintFactory),
                friday(constraintFactory),
                saturday(constraintFactory),
                sunday(constraintFactory),
                oneTeamCanAtMostDo1ShiftCInA2MonthPeriod(constraintFactory),
                oneTeamWedDayOffWillNotReoccurLessThan4Week(constraintFactory),
                oneTeamFridayDayOffWillNotReoccurLessThan4Week(constraintFactory),

                readyDate(constraintFactory),
                dueDate(constraintFactory),
                // Soft constraints
                //beforeIdealEndDate(constraintFactory),
                //afterIdealEndDate(constraintFactory),
                //tagConflict(constraintFactory),
                ////
        };
    }

    // ************************************************************************
    // Hard constraints
    // ************************************************************************

    public Constraint crewConflict(ConstraintFactory constraintFactory) {
        // A crew can do at most one maintenance job at the same time.
        return constraintFactory
                .forEachUniquePair(Job.class,
                        equal(Job::getCrew),
                        overlapping(Job::getStartDate, Job::getEndDate))
                .penalizeLong(HardSoftLongScore.ONE_HARD,
                        (job1, job2) -> DAYS.between(
                                job1.getStartDate().isAfter(job2.getStartDate())
                                        ? job1.getStartDate() : job2.getStartDate(),
                                job1.getEndDate().isBefore(job2.getEndDate())
                                        ? job1.getEndDate() : job2.getEndDate()))
                .asConstraint("Crew conflict");
    }

    public Constraint oddWeekTeamInRespectiveShiftA(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Job.class)
                .filter(j -> j.getReadyDate().getDayOfWeek() == DayOfWeek.MONDAY || j.getReadyDate().getDayOfWeek() == DayOfWeek.TUESDAY || j.getReadyDate().getDayOfWeek() == DayOfWeek.THURSDAY)
                .filter(j -> isOddWeek(j))
                .filter(job -> (job.getCrew().getName() == "T2"
                        || job.getCrew().getName() == "T4"
                        || job.getCrew().getName() == "T5"
                        || job.getCrew().getName() == "T7")
                        && job.getName() != "Shift B")
                .penalize(HardSoftLongScore.ONE_HARD)
                .asConstraint("ow Wrong Team in Shift B");
    }

    public Constraint oddWeekTeamInRespectiveShiftB(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Job.class)
                .filter(j -> j.getReadyDate().getDayOfWeek() == DayOfWeek.MONDAY || j.getReadyDate().getDayOfWeek() == DayOfWeek.TUESDAY || j.getReadyDate().getDayOfWeek() == DayOfWeek.THURSDAY)
                .filter(j -> isOddWeek(j))
                .filter(job -> (job.getCrew().getName() == "T1"
                        || job.getCrew().getName() == "T3"
                        || job.getCrew().getName() == "T6"
                        || job.getCrew().getName() == "T8")
                        && job.getName() != "Shift A" )
                .penalize(HardSoftLongScore.ONE_HARD)
                .asConstraint("ow Wrong Team A");
    }

    public Constraint evenWeekTeamInRespectiveShiftA(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Job.class)
                .filter(j -> j.getReadyDate().getDayOfWeek() == DayOfWeek.MONDAY || j.getReadyDate().getDayOfWeek() == DayOfWeek.TUESDAY || j.getReadyDate().getDayOfWeek() == DayOfWeek.THURSDAY)
                .filter(j -> !isOddWeek(j))
                .filter(job -> (job.getCrew().getName() == "T2"
                        || job.getCrew().getName() == "T4"
                        || job.getCrew().getName() == "T5"
                        || job.getCrew().getName() == "T7")
                        && job.getName() != "Shift A")
                .penalize(HardSoftLongScore.ONE_HARD)
                .asConstraint("ew Wrong Team in Shift B");
    }

    public Constraint evenWeekTeamInRespectiveShiftB(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Job.class)
                .filter(j -> j.getReadyDate().getDayOfWeek() == DayOfWeek.MONDAY || j.getReadyDate().getDayOfWeek() == DayOfWeek.TUESDAY || j.getReadyDate().getDayOfWeek() == DayOfWeek.THURSDAY)
                .filter(j -> !isOddWeek(j))
                .filter(job -> (job.getCrew().getName() == "T1"
                        || job.getCrew().getName() == "T3"
                        || job.getCrew().getName() == "T6"
                        || job.getCrew().getName() == "T8")
                        && job.getName() != "Shift B" )
                .penalize(HardSoftLongScore.ONE_HARD)
                .asConstraint("ew Wrong Team A");
    }

    public Constraint wednesday(ConstraintFactory constraintFactory) {
        var wednesdayJobs = constraintFactory.forEach(Job.class).filter(j -> j.getReadyDate().getDayOfWeek() == DayOfWeek.WEDNESDAY && j.getName() == "Shift A");

        return constraintFactory.forEach(Job.class)
            .filter(j -> j.getStartDate().getDayOfWeek() == DayOfWeek.TUESDAY && j.getName() == "Shift B")
            .join(wednesdayJobs, equal(Job::getWeekNo), equal(Job::getCrew))
            .penalize(HardSoftLongScore.ofSoft(1000000))
            .asConstraint("wednesday");
    }

    public Constraint friday(ConstraintFactory constraintFactory) {
        var dayOffAndShiftAJobs = constraintFactory.forEach(Job.class)
            .filter(j -> j.getReadyDate().getDayOfWeek() == DayOfWeek.FRIDAY 
            && (j.getName() == "Shift A" || j.getName() == "Day Off"));

        return constraintFactory.forEach(Job.class)
            .filter(j -> j.getStartDate().getDayOfWeek() == DayOfWeek.THURSDAY && j.getName() == "Shift B")
            .join(dayOffAndShiftAJobs, equal(Job::getWeekNo), equal(Job::getCrew))
            .penalize(HardSoftLongScore.ofSoft(1000000))
            .asConstraint("friday");
    }

    public Constraint oneTeamCanAtMostDo1ShiftCInA2MonthPeriod(ConstraintFactory constraintFactory) {
        var shiftCJobs = constraintFactory.forEach(Job.class).filter(j -> j.getName() == "Shift C");
        return constraintFactory.forEach(Job.class).filter(j -> j.getName() == "Shift C").join(shiftCJobs)
            .filter((x, y) -> x.getCrew().getName() == y.getCrew().getName() 
            && x.getWeekNo() != y.getWeekNo()
            && Math.abs(x.getWeekNo() - y.getWeekNo()) < 8 )
            .penalize(HardSoftLongScore.ofSoft(1100000))
            .asConstraint("oneTeamCanAtMostDo1ShiftCInA2MonthPeriod");
    }

    public Constraint oneTeamWedDayOffWillNotReoccurLessThan4Week(ConstraintFactory constraintFactory) {
        var WedDayOffJobs = constraintFactory.forEach(Job.class).filter(j -> j.getReadyDate().getDayOfWeek() == DayOfWeek.WEDNESDAY && j.getName() == "Day Off");
        return constraintFactory.forEach(Job.class).filter(j -> j.getReadyDate().getDayOfWeek() == DayOfWeek.WEDNESDAY && j.getName() == "Day Off").join(WedDayOffJobs)
            .filter((x, y) -> x.getCrew().getName() == y.getCrew().getName() 
            && x.getWeekNo() != y.getWeekNo()
            && Math.abs(x.getWeekNo() - y.getWeekNo()) < 4 )
            .penalize(HardSoftLongScore.ofSoft(1100000))
            .asConstraint("oneTeamWedDayOffWillNotReoccurLessThan4Week");
    }

    public Constraint oneTeamFridayDayOffWillNotReoccurLessThan4Week(ConstraintFactory constraintFactory) {
        var FridayDayOffJobs = constraintFactory.forEach(Job.class).filter(j -> j.getReadyDate().getDayOfWeek() == DayOfWeek.FRIDAY && j.getName() == "Day Off");
        return constraintFactory.forEach(Job.class).filter(j -> j.getReadyDate().getDayOfWeek() == DayOfWeek.FRIDAY && j.getName() == "Day Off").join(FridayDayOffJobs)
            .filter((x, y) -> x.getCrew().getName() == y.getCrew().getName() 
            && x.getWeekNo() != y.getWeekNo()
            && Math.abs(x.getWeekNo() - y.getWeekNo()) < 4 )
            .penalize(HardSoftLongScore.ofSoft(1100000))
            .asConstraint("oneTeamFridayDayOffWillNotReoccurLessThan4Week");
    }

    public Constraint saturday(ConstraintFactory constraintFactory) {
        var saturdayJob = constraintFactory.forEach(Job.class).filter(j -> j.getReadyDate().getDayOfWeek() == DayOfWeek.SATURDAY);

        return constraintFactory.forEach(Job.class)
            .filter(j -> j.getStartDate().getDayOfWeek() == DayOfWeek.FRIDAY && j.getName() != "Day Off")
            .join(saturdayJob, equal(Job::getWeekNo), equal(Job::getCrew))
            .penalize(HardSoftLongScore.ofSoft(900000))
            .asConstraint("saturday");
    }

    public Constraint sunday(ConstraintFactory constraintFactory) {
        var sundayJob = constraintFactory.forEach(Job.class).filter(j -> j.getReadyDate().getDayOfWeek() == DayOfWeek.SUNDAY);

        return constraintFactory.forEach(Job.class)
            .filter(j -> j.getStartDate().getDayOfWeek() == DayOfWeek.FRIDAY && j.getName() != "Day Off")
            .join(sundayJob, equal(Job::getWeekNo), equal(Job::getCrew))
            .penalize(HardSoftLongScore.ofSoft(900000))
            .asConstraint("sunday");
    }

    

    public Constraint readyDate(ConstraintFactory constraintFactory) {
        // Don't start a maintenance job before its ready to start.
        return constraintFactory.forEach(Job.class)
                .filter(job -> job.getReadyDate() != null
                        && job.getStartDate().isBefore(job.getReadyDate()))
                .penalizeLong(HardSoftLongScore.ONE_HARD,
                        job -> DAYS.between(job.getStartDate(), job.getReadyDate()))
                .asConstraint("Ready date");
    }

    public Constraint dueDate(ConstraintFactory constraintFactory) {
        // Don't end a maintenance job after its due.
        return constraintFactory.forEach(Job.class)
                .filter(job -> job.getDueDate() != null
                        && job.getEndDate().isAfter(job.getDueDate()))
                .penalizeLong(HardSoftLongScore.ONE_HARD,
                        job -> DAYS.between(job.getDueDate(), job.getEndDate()))
                .asConstraint("Due date");
    }


    // ************************************************************************
    // Soft constraints
    // ************************************************************************

    public Constraint beforeIdealEndDate(ConstraintFactory constraintFactory) {
        // Early maintenance is expensive because the sooner maintenance is done, the sooner it needs to happen again.
        return constraintFactory.forEach(Job.class)
                .filter(job -> job.getIdealEndDate() != null
                        && job.getEndDate().isBefore(job.getIdealEndDate()))
                .penalizeLong(HardSoftLongScore.ofSoft(1),
                        job -> DAYS.between(job.getEndDate(), job.getIdealEndDate()))
                .asConstraint("Before ideal end date");
    }

    public Constraint afterIdealEndDate(ConstraintFactory constraintFactory) {
        // Late maintenance is risky because delays can push it over the due date.
        return constraintFactory.forEach(Job.class)
                .filter(job -> job.getIdealEndDate() != null
                        && job.getEndDate().isAfter(job.getIdealEndDate()))
                .penalizeLong(HardSoftLongScore.ofSoft(1_000_000),
                        job -> DAYS.between(job.getIdealEndDate(), job.getEndDate()))
                .asConstraint("After ideal end date");
    }
    
    public Constraint tagConflict(ConstraintFactory constraintFactory) {
        // Avoid overlapping maintenance jobs with the same tag (for example road maintenance in the same area).
        return constraintFactory
                .forEachUniquePair(Job.class,
                        overlapping(Job::getStartDate, Job::getEndDate),
                        // TODO Use intersecting() when available https://issues.redhat.com/browse/PLANNER-2558
                        filtering((job1, job2) -> !Collections.disjoint(
                                job1.getTagSet(), job2.getTagSet())))
                .penalizeLong(HardSoftLongScore.ofSoft(1_000),
                        (job1, job2) -> {
                            Set<String> intersection = new HashSet<>(job1.getTagSet());
                            intersection.retainAll(job2.getTagSet());
                            long overlap = DAYS.between(
                                    job1.getStartDate().isAfter(job2.getStartDate())
                                            ? job1.getStartDate()  : job2.getStartDate(),
                                    job1.getEndDate().isBefore(job2.getEndDate())
                                            ? job1.getEndDate() : job2.getEndDate());
                            return intersection.size() * overlap;
                        })
                .asConstraint("Tag conflict");
    }

    public boolean isOddWeek(Job job) {
        if (job.getWeekNo() % 2 == 0) {
            return true;
        }
        else {
            return false;
        }
    }

}
