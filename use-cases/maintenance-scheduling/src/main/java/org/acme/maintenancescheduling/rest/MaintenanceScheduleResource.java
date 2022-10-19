package org.acme.maintenancescheduling.rest;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.acme.maintenancescheduling.domain.Job;
import org.acme.maintenancescheduling.domain.MaintenanceSchedule;
import org.acme.maintenancescheduling.domain.WorkCalendar;
import org.acme.maintenancescheduling.persistence.CrewRepository;
import org.acme.maintenancescheduling.persistence.JobRepository;
import org.acme.maintenancescheduling.persistence.WorkCalendarRepository;
import org.optaplanner.core.api.score.ScoreManager;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.solver.SolverManager;
import org.optaplanner.core.api.solver.SolverStatus;

import com.arjuna.ats.internal.jdbc.drivers.modifiers.list;

import io.micrometer.core.instrument.Tags;
import io.quarkus.panache.common.Sort;

@Path("/schedule")
public class MaintenanceScheduleResource {

    public static final Long SINGLETON_SCHEDULE_ID = 1L;

    String[] westTC = {"Chua Chu Kang", "Holland-Bukit Pangjang", "Jurong-Clementi", "West Coast"};
    String[] centralTC = {"Bishan-Toa Payoh", "Jalan Besar", "Marine Parade", "Tanjong Pagar"};
    String[] northTC = {"Ang Mo Kio", "Marsiling-Yew Tee", "Nee Soon", "Sembawang"};
    String[] eastTC = {"Aljunied-Hougang", "East Coast", "Tampines", "Pasir Ris-Punggol", "Sengkang"};

    @Inject
    WorkCalendarRepository workCalendarRepository;
    @Inject
    CrewRepository crewRepository;
    @Inject
    JobRepository jobRepository;

    @Inject
    SolverManager<MaintenanceSchedule, Long> solverManager;
    @Inject
    ScoreManager<MaintenanceSchedule, HardSoftScore> scoreManager;

    // To try, open http://localhost:8080/schedule
    @GET
    public MaintenanceSchedule getSchedule() {
        // Get the solver status before loading the solution
        // to avoid the race condition that the solver terminates between them
        SolverStatus solverStatus = getSolverStatus();
        MaintenanceSchedule solution = findById(SINGLETON_SCHEDULE_ID);
        scoreManager.updateScore(solution); // Sets the score
        solution.setSolverStatus(solverStatus);
        return solution;
    }

    public SolverStatus getSolverStatus() {
        return solverManager.getSolverStatus(SINGLETON_SCHEDULE_ID);
    }

    @POST
    @Path("solve")
    public void solve() {
        solverManager.solveAndListen(SINGLETON_SCHEDULE_ID,
                this::findById,
                this::save);
    }

    @POST
    @Path("stopSolving")
    public void stopSolving() {
        solverManager.terminateEarly(SINGLETON_SCHEDULE_ID);
    }

    @Transactional
    protected MaintenanceSchedule findById(Long id) {
        if (!SINGLETON_SCHEDULE_ID.equals(id)) {
            throw new IllegalStateException("There is no schedule with id (" + id + ").");
        }
        return new MaintenanceSchedule(
                workCalendarRepository.listAll().get(0),
                crewRepository.listAll(Sort.by("name").and("id")),
                jobRepository.listAll(Sort.by("dueDate").and("readyDate").and("name").and("id")));
    }

    @Transactional
    protected void save(MaintenanceSchedule schedule) {
        int t1Counter = 0;
        int t2Counter = 0;
        int t3Counter = 0;
        int t4Counter = 0;
        int t5Counter = 0;
        int t6Counter = 0;
        int t7Counter = 0;
        int t8Counter = 0;
        

        for (Job job : schedule.getJobList()) {
            // TODO this is awfully naive: optimistic locking causes issues if called by the SolverManager
            Job attachedJob = jobRepository.findById(job.getId());
            attachedJob.setCrew(job.getCrew());
            attachedJob.setStartDate(job.getStartDate());
            attachedJob.setEndDate(job.getEndDate());

            if (attachedJob.getTagSet().contains("Day Off"))
                continue;
            var tagSet = attachedJob.getTagSet();
            switch (attachedJob.getCrew().getName()) {
                case "T1":
                    tagSet = removeExistingSet(tagSet);
                    tagSet.add(westTC[t1Counter % 4]);
                    t1Counter++;
                    break;
                case "T2":
                    tagSet = removeExistingSet(tagSet);
                    tagSet.add(westTC[(t2Counter + 2) % 4]);
                    t2Counter++;
                    break;
                case "T3":
                    tagSet = removeExistingSet(tagSet);
                    tagSet.add(centralTC[t3Counter % 4]);
                    t3Counter++;
                    break;
                case "T4":
                    tagSet = removeExistingSet(tagSet);
                    tagSet.add(centralTC[(t4Counter + 2) % 4]);
                    t4Counter++;
                    break;
                case "T5":
                    tagSet = removeExistingSet(tagSet);
                    tagSet.add(northTC[t5Counter % 4]);
                    t5Counter++;
                    break;
                case "T6":
                    tagSet = removeExistingSet(tagSet);
                    tagSet.add(northTC[(t6Counter + 2) % 4]);
                    t6Counter++;
                    break;
                case "T7":
                    tagSet = removeExistingSet(tagSet);
                    tagSet.add(eastTC[t7Counter % 5]);
                    t7Counter++;
                    break;
                case "T8":
                    tagSet = removeExistingSet(tagSet);
                    tagSet.add(eastTC[(t8Counter + 2) % 5]);
                    t8Counter++;
                    break;
                
            }
        }
        
        // Comparator<Job> compareCrew = (x, y) -> x.getCrew().getName().compareTo(y.getCrew().getName());
        // Comparator<Job> compareReadyDate = (x, y) -> x.getReadyDate().compareTo(y.getReadyDate());
        // var sortedJogList = jobRepository.listAll().stream().sorted()
    }

    Set<String> removeExistingSet(Set<String> tagSet) {
        for (var string : westTC) {
            tagSet.remove(string);
        }
        for (var string : centralTC) {
            tagSet.remove(string);
        }
        for (var string : northTC) {
            tagSet.remove(string);
        }
        for (var string : eastTC) {
            tagSet.remove(string);
        }
        return tagSet;
    }
}
