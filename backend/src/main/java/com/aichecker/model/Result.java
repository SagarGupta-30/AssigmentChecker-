package com.aichecker.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "results")
public class Result {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false, unique = true)
    private Submission submission;

    @Column(nullable = false)
    private Integer correctCount;

    @Column(nullable = false)
    private Integer wrongCount;

    @Column(nullable = false)
    private Integer notAttemptedCount;

    @Column(nullable = false)
    private Double percentage;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String detailsJson;

    @Column(nullable = false)
    private Instant evaluatedAt;

    @PrePersist
    void onCreate() {
        this.evaluatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Submission getSubmission() {
        return submission;
    }

    public void setSubmission(Submission submission) {
        this.submission = submission;
    }

    public Integer getCorrectCount() {
        return correctCount;
    }

    public void setCorrectCount(Integer correctCount) {
        this.correctCount = correctCount;
    }

    public Integer getWrongCount() {
        return wrongCount;
    }

    public void setWrongCount(Integer wrongCount) {
        this.wrongCount = wrongCount;
    }

    public Integer getNotAttemptedCount() {
        return notAttemptedCount;
    }

    public void setNotAttemptedCount(Integer notAttemptedCount) {
        this.notAttemptedCount = notAttemptedCount;
    }

    public Double getPercentage() {
        return percentage;
    }

    public void setPercentage(Double percentage) {
        this.percentage = percentage;
    }

    public String getDetailsJson() {
        return detailsJson;
    }

    public void setDetailsJson(String detailsJson) {
        this.detailsJson = detailsJson;
    }

    public Instant getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(Instant evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }
}
