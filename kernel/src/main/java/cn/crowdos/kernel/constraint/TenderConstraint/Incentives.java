package cn.crowdos.kernel.constraint.TenderConstraint;

import cn.crowdos.kernel.algorithms.AlgoFactoryAdapter;
import cn.crowdos.kernel.resource.Task;
import cn.crowdos.kernel.system.SystemResourceCollection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class Incentives extends AlgoFactoryAdapter  {
    public Incentives(SystemResourceCollection resourceCollection) {
        super(resourceCollection);
    }

    /**
     * list 只有一个对象 值选择一个科研团队 固定价格  交易
     * @param event
     * @param tenderParticipants
     * @return
     */
    private List<TenderParticipant> maxauction(Task event, List<TenderParticipant> tenderParticipants) {
        TenderParticipant winner=
                tenderParticipants.stream().max((o1, o2)
                        -> (int) (o1.getAchievementScore()-o2.getAchievementScore())).get();

        return List.of(winner);
    }

    /**
     * 选择一个科研团队实现效用最大化 没有成本限制的竞标 vcg机制
     * @param task
     * @param tenderParticipants
     * @return
     */
    private List<TenderParticipant> vcgauction(Task task, List<TenderParticipant> tenderParticipants) {
        List<TenderParticipant> winners=new ArrayList<>();
        TenderParticipant bestWin=null;
        double maxWelfare=Integer.MIN_VALUE;
        //计算得到最大社会福利的科研团队
        for (TenderParticipant tenderParticipant:tenderParticipants){
            double welfare=100*Math.log(10*tenderParticipant.getAchievementScore())-tenderParticipant.getBid();
            if (welfare>maxWelfare){
                maxWelfare=welfare;
                bestWin=tenderParticipant;
            }
        }
        //计算该科研团队不存在时的最大社会福利，并作为支付价格整体类似于二价拍卖
        double TmpMaxWelfare=Integer.MIN_VALUE;
        for (TenderParticipant tenderParticipant:tenderParticipants){
            if (tenderParticipant.equals(bestWin)){
                continue;
            }
            double welfare=100*Math.log(10*tenderParticipant.getAchievementScore())-tenderParticipant.getBid();
            TmpMaxWelfare=Math.max(TmpMaxWelfare,welfare);

        }
        double payMent=TmpMaxWelfare;
        bestWin.setSalary(payMent);
        winners.add(bestWin);
        return winners;
    }

    /**
     * 带有预算的效用最大化拍卖
     * @param event
     * @param tenderParticipants
     * @param budget
     * @return 获胜者 状态为1 并设置了报酬
     */
    private List<TenderParticipant> bfdauction(Task  event, List<TenderParticipant> tenderParticipants,Double budget) {

        //初始化支付
        HashMap<Integer,Double> salrys=new HashMap<>();
        // 过滤报价大于预算的投标者
        List<TenderParticipant> bidders =
                tenderParticipants.stream().filter(tenderParticipant -> tenderParticipant.getBid() < budget).collect(Collectors.toList());
        List<TenderParticipant> biddderCopy=new ArrayList<>(bidders);
        List<TenderParticipant> winners=new ArrayList<>();
        //设置随机数实现随机机制
        double r=Math.random();
        //40的概率选择具有最大效用的科研团队并设置支付报酬为预算
        if (r<0.4){
            TenderParticipant winner=bidders.stream().max(new Comparator<TenderParticipant>() {
                @Override
                public int compare(TenderParticipant o1, TenderParticipant o2) {
                    return (int) (o2.getAchievementScore()-o1.getAchievementScore());
                }
            }).get();
            winner.setSalary(budget);
            winners.add(winner);
            return winners;
        }
        // 60的概率执行带有预算的子模最大化贪心算法
        double max=0;
        TenderParticipant maxTeam=null;
        //计算出具有最大平均边际效用的科研团队
        for (TenderParticipant tenderParticipant:bidders){
            double comUitity=100*Math.log(10*tenderParticipant.getAchievementScore())/tenderParticipant.getBid();
            if (comUitity>max){
                maxTeam=tenderParticipant;
                max=comUitity;
            }
        }
        if (maxTeam==null){
            return winners;
        }
        //判断是否满足预算约束
        boolean flag=maxTeam.getBid()<budget/2;
        // 循环计算满足预算约束的平均边际效用最大的科研团队添加获胜者集合中
        while (bidders.size()>0&&flag){
            //更新获胜者集合和投标者集合
            winners.add(maxTeam);
            bidders.remove(maxTeam);
            double pre=compute(winners);
            max=0;
            maxTeam=null;
            double curUit=0;
            // 计算平均边际效用最大的科研团队
            for (TenderParticipant team_event:bidders){
                curUit=compute(winners,team_event);
                double marignUitity=(curUit-pre)/team_event.getBid();
                if (marignUitity>max){
                    max=marignUitity;
                    maxTeam=team_event;
                }
            }
            double maxCur=max*maxTeam.getBid();
            flag=maxTeam.getBid()<=budget*maxCur/(2*curUit);
        }
        // 执行支付报酬计算逻辑
        for (TenderParticipant winner:winners){
            //获取不包含该中标者的候选集合
            List<TenderParticipant> tmpTeam=new ArrayList<>(biddderCopy);
            tmpTeam.remove(winner);
            List<TenderParticipant> tmpWin=new ArrayList<>();
            double tmpMax=0;
            TenderParticipant tmpMaxTeam=null;
            //执行上述计算获胜者逻辑
            for (TenderParticipant t:tmpTeam){
                double comUitity=Math.log(10*t.getAchievementScore())/t.getBid();
                if (comUitity>tmpMax){
                    tmpMaxTeam=t;
                    tmpMax=comUitity;
                }
            }
            boolean tmpflag=tmpMaxTeam.getBid()<budget/2;
            while (tmpTeam.size()>0&&tmpflag){
                double pre=compute(tmpWin);
                tmpMax=0;
                tmpMaxTeam=null;
                double curUit=0;
                for (TenderParticipant p:tmpTeam){
                    curUit=compute(tmpWin,p);
                    double marignUitity=(curUit-pre)/p.getBid();
                    if (marignUitity>tmpMax){
                        tmpMax=marignUitity;
                        tmpMaxTeam=p;
                    }
                }
                double curTmpMax=tmpMax*tmpMaxTeam.getBid();
                double fis=compute(tmpWin,winner)-compute(tmpWin);
                // 根据临界价格公式更新获胜者支付报酬
                salrys.put(Math.toIntExact(winner.getId()),Math.max(salrys.getOrDefault(winner.getId(),  0.0)
                        ,Math.min(budget*fis/(2*compute(tmpWin,winner)),
                                fis*tmpMaxTeam.getBid()/curTmpMax)));
                tmpflag=tmpMaxTeam.getBid()<=budget*curTmpMax/(2*curUit);

                tmpWin.add(tmpMaxTeam);
                tmpTeam.remove(tmpMaxTeam);
            }
        }
        // 更新结果集合
        for (TenderParticipant winner :winners){
            winner.setSalary(salrys.get(winner.getId()));
        }
        return winners;
    }

    // 计算企业对接科研团队的效用
    private double compute(List<TenderParticipant> winners, TenderParticipant tenderParticipant) {

        double totalScore=0;
        for (TenderParticipant t:winners){
            totalScore+=t.getAchievementScore();
        }
        totalScore+=tenderParticipant.getAchievementScore();
        return 100*Math.log(10*totalScore);
    }
    // 计算企业对接科研团队的效用
    private double compute(List<TenderParticipant> S) {
        if(S.isEmpty()){
            return 0;
        }
        double totalScore=0;
        for (TenderParticipant tenderParticipant:S){
            totalScore+=tenderParticipant.getAchievementScore();
        }
        return  100*Math.log(10*totalScore);
    }



}
