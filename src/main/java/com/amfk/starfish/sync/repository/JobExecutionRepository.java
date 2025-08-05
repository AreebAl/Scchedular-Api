package com.amfk.starfish.sync.repository;

import com.amfk.starfish.sync.entity.JobExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobExecutionRepository extends JpaRepository<JobExecution, Long> {
    
    List<JobExecution> findByJobNameOrderByCreateTsDesc(String jobName);
    
    List<JobExecution> findByStatusOrderByCreateTsDesc(JobExecution.JobStatus status);
    
    Optional<JobExecution> findFirstByJobNameOrderByCreateTsDesc(String jobName);
    
    @Query("SELECT je FROM JobExecution je WHERE je.jobName = :jobName AND je.createTs >= :startDate ORDER BY je.createTs DESC")
    List<JobExecution> findByJobNameAndCreateTsAfter(@Param("jobName") String jobName, @Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT COUNT(je) FROM JobExecution je WHERE je.jobName = :jobName AND je.status = :status AND je.createTs >= :startDate")
    long countByJobNameAndStatusAndCreateTsAfter(@Param("jobName") String jobName, 
                                                @Param("status") JobExecution.JobStatus status, 
                                                @Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT je FROM JobExecution je WHERE je.status = 'RUNNING'")
    List<JobExecution> findRunningJobs();
} 