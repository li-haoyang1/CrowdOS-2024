package cn.crowdos.kernel.constraint.TenderConstraint;

import cn.crowdos.kernel.constraint.Condition;
import cn.crowdos.kernel.resource.Participant;

// 实现Participant类，并多报价bid和酬劳salary属性
public class TenderParticipant implements Participant {
    //报价
    private double bid;
    //酬劳
    private double salary;
    //报价参与者的id
    private int id;
    //能力分
    private double  achievementScore;

    public double getBid() {
        return bid;
    }

    public void setBid(double bid) {
        this.bid = bid;
    }

    public double getAchievementScore(){
        return achievementScore;
    }

    public void setAchievementScore(double achievementScore){
        this.achievementScore=achievementScore;
    }
    public int getId(){
        return id;
    }
    public void setId(int id){
        this.id=id;
    }


    public double getSalary() {
        return salary;
    }

    public void setSalary(double salary) {
        this.salary = salary;
    }

    public TenderParticipant(double bid, double salary) {
        this.bid = bid;
        this.salary = salary;
    }

    public TenderParticipant() {
    }

    @Override
    public ParticipantStatus getStatus() {
        return null;
    }

    @Override
    public void setStatus(ParticipantStatus status) {

    }
    @Override
    public boolean hasAbility(Class<? extends Condition> conditionClass) {
        return false;
    }

    @Override
    public Condition getAbility(Class<? extends Condition> conditionClass) {
        return null;
    }

    @Override
    public boolean available() {
        return false;
    }
}
