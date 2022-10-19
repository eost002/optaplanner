package org.acme.maintenancescheduling.bootstrap;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.chrono.IsoChronology;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.acme.maintenancescheduling.domain.Crew;
import org.acme.maintenancescheduling.domain.Job;
import org.acme.maintenancescheduling.domain.WorkCalendar;
import org.acme.maintenancescheduling.persistence.CrewRepository;
import org.acme.maintenancescheduling.persistence.JobRepository;
import org.acme.maintenancescheduling.persistence.WorkCalendarRepository;
import org.acme.maintenancescheduling.solver.EndDateUpdatingVariableListener;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class DemoDataGenerator {

    @ConfigProperty(name = "schedule.demoData", defaultValue = "SMALL")
    public DemoData demoData;

    public enum DemoData {
        NONE,
        SMALL,
        LARGE
    }

    @Inject
    WorkCalendarRepository workCalendarRepository;
    @Inject
    CrewRepository crewRepository;
    @Inject
    JobRepository jobRepository;

    @Transactional
    public void generateDemoData(@Observes StartupEvent startupEvent) {
        if (demoData == DemoData.NONE) {
            return;
        }

        List<Crew> crewList = new ArrayList<>();
        crewList.add(new Crew("T1"));
        crewList.add(new Crew("T3"));
        crewList.add(new Crew("T6"));
        crewList.add(new Crew("T8"));
        crewList.add(new Crew("T2"));
        crewList.add(new Crew("T4"));
        crewList.add(new Crew("T5"));
        crewList.add(new Crew("T7"));
        if (demoData == DemoData.LARGE) {
            crewList.add(new Crew("Delta crew"));
            crewList.add(new Crew("Epsilon crew"));
        }
        crewRepository.persist(crewList);

        int jobListSize = 448; // 56 = One week of jobs
        LocalDate fromDate = LocalDate.now().withMonth(12).withDayOfMonth(29).with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        int weekListSize = (jobListSize / 56) + (jobListSize % 56 > 0 ? 1 : 0);
        LocalDate toDate = fromDate.plusWeeks(weekListSize);
        workCalendarRepository.persist(new WorkCalendar(fromDate, toDate));

        final String[] JOB_AREA_NAMES = {
                "Shift A", "Shift B", "Shift C", "Day Off", "Weekend Shift"};
        

        List<Job> jobList = new ArrayList<>();

        
        int case2Counter = 0;
        int case4Counter = 0;
        int case5Counter = 0;
        int case6Counter = 0;
        for (int i = 0; i < jobListSize; i++) {
            int noOfWeeksCompleted = i / 56;
            int dayOffsetInWeek = (i % 56) / 8; // 0 = Monday
            if (i % 56 == 0) {
                case2Counter = 0;
                case4Counter = 0;
                case5Counter = 0;
                case6Counter = 0;
            }
            int dayOffsetFromInitial = (noOfWeeksCompleted * 7) + dayOffsetInWeek;
            int weekNo = dayOffsetFromInitial / 7;
            switch (dayOffsetInWeek) {
                case 0:
                case 1:
                case 3:
                    String jobArea = JOB_AREA_NAMES[i % 2];
                    LocalDate readyDate = EndDateUpdatingVariableListener.calculateEndDate(fromDate, dayOffsetFromInitial);
                    LocalDate dueDate = EndDateUpdatingVariableListener.calculateEndDate(readyDate, 1);
                    LocalDate idealEndDate = EndDateUpdatingVariableListener.calculateEndDate(readyDate, 1);
                    Set<String> tagSet = Set.of(jobArea);
                    jobList.add(new Job(jobArea, 1, readyDate, dueDate, idealEndDate, tagSet, weekNo));
                    break;
                case 2:
                    jobArea = JOB_AREA_NAMES[case2Counter < 4 ? 0 : (case2Counter < 6 ? 1 : 3)];
                    case2Counter++;
                    readyDate = EndDateUpdatingVariableListener.calculateEndDate(fromDate, dayOffsetFromInitial);
                    dueDate = EndDateUpdatingVariableListener.calculateEndDate(readyDate, 1);
                    idealEndDate = EndDateUpdatingVariableListener.calculateEndDate(readyDate, 1);
                    tagSet = Set.of(jobArea);
                    jobList.add(new Job(jobArea, 1, readyDate, dueDate, idealEndDate, tagSet, weekNo));
                    break;
                case 4:
                    jobArea = JOB_AREA_NAMES[case4Counter < 3 ? 1 : (case4Counter < 5 ? 0 : (case4Counter < 7 ? 3 : 2))];
                    case4Counter++;
                    readyDate = EndDateUpdatingVariableListener.calculateEndDate(fromDate, dayOffsetFromInitial);
                    dueDate = EndDateUpdatingVariableListener.calculateEndDate(readyDate, 1);
                    idealEndDate = EndDateUpdatingVariableListener.calculateEndDate(readyDate, 1);
                    tagSet = Set.of(jobArea);
                    jobList.add(new Job(jobArea, 1, readyDate, dueDate, idealEndDate, tagSet, weekNo));
                    break;
                case 5:
                    if (case5Counter > 1)
                        break;
                    case5Counter++;
                    jobArea = JOB_AREA_NAMES[4];
                    readyDate = EndDateUpdatingVariableListener.calculateEndDate(fromDate, dayOffsetFromInitial);
                    dueDate = EndDateUpdatingVariableListener.calculateEndDate(readyDate, 1);
                    idealEndDate = EndDateUpdatingVariableListener.calculateEndDate(readyDate, 1);
                    tagSet = Set.of(jobArea);
                    jobList.add(new Job(jobArea, 1, readyDate, dueDate, idealEndDate, tagSet, weekNo));
                    break;
                case 6:
                    if (case6Counter > 1)
                        break;
                    case6Counter++;
                    jobArea = JOB_AREA_NAMES[4];
                    readyDate = EndDateUpdatingVariableListener.calculateEndDate(fromDate, dayOffsetFromInitial);
                    dueDate = EndDateUpdatingVariableListener.calculateEndDate(readyDate, 1);
                    idealEndDate = EndDateUpdatingVariableListener.calculateEndDate(readyDate, 1);
                    tagSet = Set.of(jobArea);
                    jobList.add(new Job(jobArea, 1, readyDate, dueDate, idealEndDate, tagSet, weekNo));
                    break;
            }
            
        }
        
        jobRepository.persist(jobList);
    }

}
