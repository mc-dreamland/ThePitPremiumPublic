package cn.charlotte.pit.activity;

import cn.charlotte.pit.activity.genes.data.GenesPlayerData;
import cn.charlotte.pit.activity.newyear.NewYearData;
import cn.charlotte.pit.data.sub.GenesisData;
import cn.charlotte.pit.menu.Help.button.UpdateMenu;
import lombok.Data;

@Data
public class ActivityPlayerData {

    private Double ver = 0.0;
    private NewYearData newYearData = new NewYearData();
    private GenesPlayerData genesisData = new GenesPlayerData();

//    public ActivityPlayerData() {
//        this.clear();
//    }

    public void clear() {
        this.newYearData = new NewYearData();
        this.ver = UpdateMenu.getVer();
        this.genesisData = new GenesPlayerData();
    }

    public void purposeCleaning(){
        if(this.ver != null && this.ver != UpdateMenu.getVer()) this.clear();
    }
}
