package giacobbe.alessio.firetest;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by assas on 31/10/2016.
 */

public class locationadapter extends ArrayAdapter<Location> {
    public locationadapter(Context context, ArrayList<Location> locations) {
        super(context,0,locations);
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        Location loc = getItem(position);

        convertView = LayoutInflater.from(getContext()).inflate(R.layout.locationslist, parent, false);
        TextView titolo = (TextView) convertView.findViewById(R.id.listtile);
        TextView sub = (TextView) convertView.findViewById(R.id.listsub);
        titolo.setText(loc.Title);
        sub.setText("lat: " + loc.Latitude + " - long: " + loc.Longitude);
        return convertView;
    }
}
