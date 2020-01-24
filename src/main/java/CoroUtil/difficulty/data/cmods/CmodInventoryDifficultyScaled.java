package CoroUtil.difficulty.data.cmods;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import CoroUtil.difficulty.UtilEntityBuffs;
import CoroUtil.difficulty.data.DataCmod;
import CoroUtil.difficulty.data.DifficultyDataReader;

public class CmodInventoryDifficultyScaled extends DataCmod implements JsonSerializer {
    public List<CmodInventoryEntry> listInventories = new ArrayList<>();

    @Override
    public JsonElement serialize(Object src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();
        obj.addProperty("cmod", cmod);
        JsonArray arr = new JsonArray();
        for (CmodInventoryEntry entry : listInventories) {

            //use existing serializer to get the inventory parts
            JsonObject objInv = ((new JsonParser()).parse((new Gson()).toJson(entry.inventory, DifficultyDataReader.lookupJsonNameToCmodDeserializer.get(UtilEntityBuffs.dataEntityBuffed_Inventory))).getAsJsonObject());

            //add our extras
            objInv.addProperty("min", entry.min);
            objInv.addProperty("max", entry.max);

            //cleanup for spec, remove cmod name to match our intended spec, it was added since were just using built in serializer to merge things together
            //real cmod field is outside array
            objInv.remove("cmod");

            arr.add(objInv);
        }
        obj.add("stages", arr);
        return obj;
    }

    @Override
    public String toString() {
        String srcString = super.toString() + ", items: ";

        for (CmodInventoryEntry entry : listInventories) {
            srcString += entry.toString();
        }

        return srcString;
    }
}
