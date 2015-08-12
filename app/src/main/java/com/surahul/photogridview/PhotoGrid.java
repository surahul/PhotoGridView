package com.surahul.photogridview;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.surahul.photogridview.internal.SquareImageView;
import com.surahul.photogridview.internal.Utils;
import com.tonicartos.superslim.GridSLM;

import java.util.ArrayList;



/**
 * Created by workhard on 7/23/15.
 */
public class PhotoGrid extends RecyclerView {



    private static final int DEFAULT_NUM_OF_COLUMNS = 3;
    private static final boolean DEFAULT_HAS_HEADERS = false;
    private static final boolean DEFAULT_HAS_STICKY_HEADERS = false;
    private static final ZImageGridMode DEFAULT_MODE = ZImageGridMode.grid;
    private static final boolean DEFAULT_AUTO_CONTENT_INSET = true;



    public enum ZImageGridMode{
        grid,hsv
    }

    public void setOnItemClickListener(OnItemClickListener listener){
        this.onItemClickListener = listener;
    }


    public interface OnItemClickListener{
        void onItemClick(int position, View view);
    }





    public BaseAdapter adapter;
    ZImageGridMode mode = DEFAULT_MODE;
    private OnItemClickListener onItemClickListener;
    int numberOfColumns = DEFAULT_NUM_OF_COLUMNS;
    boolean hasHeaders = DEFAULT_HAS_HEADERS;
    boolean hasStickyHeaders = DEFAULT_HAS_STICKY_HEADERS;
    boolean autoContentInset = DEFAULT_AUTO_CONTENT_INSET;


    ArrayList<Integer> sectionMaxData = new ArrayList<>();
    ArrayList<Integer> sectionMaxLabelNumbers = new ArrayList<>();

    public void setSectionMaxItemNumbers(int... sectionMaxItemNumbers){
        this.sectionMaxData.clear();
        for(int i : sectionMaxItemNumbers)
            this.sectionMaxData.add(i);
        notifyDataSetChanged();
    }
    public void setSectionMaxRowNumbers(int... sectionMaxRowNumbers){
        this.sectionMaxData.clear();
        for(int i : sectionMaxRowNumbers)
            this.sectionMaxData.add(i*getNumberOfColumns());
        notifyDataSetChanged();
    }
    public void setHSVMaxItemNumbers(int hsvMaxItem) {
        if(adapter instanceof HSVAdapter)
        {
            ((HSVAdapter)adapter).maxNumber = hsvMaxItem;
            notifyDataSetChanged();
        }
    }

    public void setSectionMaxLabelNumbers(int... sectionMaxLabelNumbers){
        for(int i : sectionMaxLabelNumbers)
            this.sectionMaxLabelNumbers.add(i);
        notifyDataSetChanged();
    }


    public PhotoGrid(Context context) {
        super(context);
        initialize();
    }

    public PhotoGrid(Context context, AttributeSet attrs) {
        super(context, attrs);
        getStuffFromXml(attrs);
        initialize();
    }

    public PhotoGrid(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        getStuffFromXml(attrs);
        initialize();
    }

    void getStuffFromXml(AttributeSet attrs){

        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.PhotoGrid);
        hasHeaders = a.getBoolean(R.styleable.PhotoGrid_photogrid_has_section_headers,DEFAULT_HAS_HEADERS);
        hasStickyHeaders = a.getBoolean(R.styleable.PhotoGrid_photogrid_sticky_headers,DEFAULT_HAS_STICKY_HEADERS);
        numberOfColumns = a.getInt(R.styleable.PhotoGrid_photogrid_num_of_columns, DEFAULT_NUM_OF_COLUMNS);
        autoContentInset = a.getBoolean(R.styleable.PhotoGrid_photogrid_auto_inset, DEFAULT_AUTO_CONTENT_INSET);

        int modeIndex = a.getInt(R.styleable.PhotoGrid_photogrid_mode,0);
        switch(modeIndex){
            case 0:
                mode = ZImageGridMode.grid;
                break;
            case 1:
                mode = ZImageGridMode.hsv;
                break;
        }


        a.recycle();
    }

    public void setData(ArrayList<PhotoGridDisplayable> data){
        this.adapter.data = data;

    }

    private void initialize(){
        int leftPadding = (int)getResources().getDimension(R.dimen.photogrid_spacing)*(autoContentInset?0:-1);
        int rightPadding = leftPadding;
        int topPadding = getPaddingTop();
        int bottomPadding = getPaddingBottom();
        setPadding(leftPadding, topPadding, rightPadding, bottomPadding);


        switch (mode){

            case grid:
                setLayoutManager(new com.tonicartos.superslim.LayoutManager(getContext()));
                adapter = new GridAdapter();
                break;
            case hsv:
                LinearLayoutManager manager = new LinearLayoutManager(getContext(),LinearLayoutManager.HORIZONTAL, false);
                setLayoutManager(manager);
                adapter = new HSVAdapter();
                break;
        }
        setAdapter(adapter);
    }

    public void notifyDataSetChanged(){
        adapter.buildListItems();
        adapter.notifyDataSetChanged();
    }






    private int getNumberOfColumns(){
        return numberOfColumns;
    }



    public abstract class BaseAdapter extends Adapter<ViewHolder>{

        public ArrayList<PhotoGridDisplayable> data;

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return null;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {

        }

        @Override
        public int getItemCount() {
            return 0;
        }

        abstract void buildListItems();
    }

    private class GridAdapter extends BaseAdapter {

        private static final int VIEW_TYPE_HEADER = 0;
        private static final int VIEW_TYPE_IMAGE = 1;
        private static final int VIEW_TYPE_OVERFLOW_IMAGE = 2;

        ArrayList<Integer> sectionItemCounts = new ArrayList<>();


        private class ListItem{
            public int viewType;
            public PhotoGridDisplayable data;
            public int sectionFirstPosition;
            public int sectionIndex;
            public int indexInSection;
            public int indexInData;

            public ListItem(PhotoGridDisplayable data,int viewType, int sectionFirstPosition,int sectionIndex,int indexInSection,int indexInData){
                this.data = data;
                this.viewType = viewType;
                this.sectionFirstPosition = sectionFirstPosition;
                this.sectionIndex = sectionIndex;
                this.indexInSection = indexInSection;
                this.indexInData = indexInData;
            }
        }

        @Override
        void buildListItems(){
            if(data==null)
                return;
            items = new ArrayList<>();
            sectionItemCounts.clear();
            if(!hasHeaders){
                for(int i=0;i<data.size();i++)
                    items.add(new ListItem(data.get(i),VIEW_TYPE_IMAGE,0,0,i,i));
                sectionItemCounts.add(data.size());
            }else{
                String lastSectionIdentifier = "";
                int headerCount = 0;
                int sectionFirstPosition = 0;
                int maxForCurrentSection = Integer.MAX_VALUE;
                int indexForMaxData = 0;
                int indexForSection = 0;

                int currentSectionIndex = -1;
                int currentSectionCount = 0;

                for(int i=0;i<data.size();i++){



                    PhotoGridDisplayable photoGridDisplayable = data.get(i);
                    if(!lastSectionIdentifier.equals(photoGridDisplayable.getSectionIdentifier())){
                        currentSectionCount = 0;
                        currentSectionIndex++;
                        sectionItemCounts.add(0);
                        sectionFirstPosition = indexForSection+headerCount;
                        ListItem itemHeader = new ListItem(photoGridDisplayable,VIEW_TYPE_HEADER,sectionFirstPosition,currentSectionIndex,-1,-1);
                        items.add(itemHeader);
                        headerCount++;
                        lastSectionIdentifier = photoGridDisplayable.getSectionIdentifier();
                        if(indexForMaxData<sectionMaxData.size()){
                            maxForCurrentSection = i+sectionMaxData.get(indexForMaxData);
                        }else
                            maxForCurrentSection = Integer.MAX_VALUE;

                        indexForMaxData++;

                    }
                    currentSectionCount++;
                    sectionItemCounts.set(currentSectionIndex,currentSectionCount);




                    if(i>maxForCurrentSection-1)
                        continue;
                    ListItem item = new ListItem(photoGridDisplayable,i==(maxForCurrentSection-1)?VIEW_TYPE_OVERFLOW_IMAGE:VIEW_TYPE_IMAGE,sectionFirstPosition,currentSectionIndex,currentSectionCount-1,i);
                    items.add(item);
                    indexForSection++;

                }



            }



        }

        private ArrayList<ListItem> items;


        public GridAdapter(){
             super();
        }


        @Override
        public int getItemViewType(int position) {
            if(items==null)
                return VIEW_TYPE_IMAGE;
            return items.get(position).viewType;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            View itemView;

            switch(viewType){
                case VIEW_TYPE_IMAGE:
                    itemView = LayoutInflater.
                            from(parent.getContext()).
                            inflate(R.layout.item_photogrid, parent, false);
                    return new ImageViewHolder(itemView);
                case VIEW_TYPE_HEADER:
                    itemView = LayoutInflater.
                            from(parent.getContext()).
                            inflate(R.layout.item_photogrid_header, parent, false);
                    return new HeaderHolder(itemView);
                case VIEW_TYPE_OVERFLOW_IMAGE:
                    itemView = LayoutInflater.
                            from(parent.getContext()).
                            inflate(R.layout.item_photogrid_overflow, parent, false);
                    return new OverflowImageViewHolder(itemView);
            }

            return null;

        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, int position) {


            final View itemView = viewHolder.itemView;
            com.tonicartos.superslim.LayoutManager.LayoutParams params;



            params = GridSLM.LayoutParams.from(itemView.getLayoutParams());
            params.setSlm(GridSLM.ID);

            //((GridSLM.LayoutParams)params ).setColumnWidth(R.dimen.column_width);


            if(getItemViewType(position)==VIEW_TYPE_HEADER){
                ((GridSLM.LayoutParams)params).setNumColumns(getNumberOfColumns());
                ((GridSLM.LayoutParams)params).isHeader = true;
                if(!hasStickyHeaders){
                    ((GridSLM.LayoutParams)params).headerDisplay &=~com.tonicartos.superslim.LayoutManager.LayoutParams.HEADER_STICKY;
                }



                HeaderHolder headerHolder = (HeaderHolder) viewHolder;
                headerHolder.bind(items.get(position));
                headerHolder.textView.setText(items.get(position).data.getSectionIdentifier());

            } else if(getItemViewType(position)==VIEW_TYPE_IMAGE){
                ((GridSLM.LayoutParams)params).setNumColumns(getNumberOfColumns());
                ImageViewHolder imageViewHolder = (ImageViewHolder)viewHolder;
                imageViewHolder.bind(items.get(position));
            } else if(getItemViewType(position)==VIEW_TYPE_OVERFLOW_IMAGE){
                ((GridSLM.LayoutParams)params).setNumColumns(getNumberOfColumns());
                OverflowImageViewHolder overflowImageViewHolder = (OverflowImageViewHolder)viewHolder;
                overflowImageViewHolder.bind(items.get(position));
            }

            params.setFirstPosition(items.get(position).sectionFirstPosition);
            itemView.setLayoutParams(params);



        }



        @Override
        public int getItemCount() {
            if(items==null)
                return 0;
            return items.size();
        }

        private class ImageViewHolder extends ViewHolder{

            SquareImageView imageView;
            public ImageViewHolder(View itemView) {
                super(itemView);
                this.imageView = (SquareImageView)itemView.findViewById(R.id.image_view);
            }

            public void bind(ListItem item){

                try{
                    imageView.setImageResource(Integer.parseInt(item.data.getImageUri()));
                }catch (Exception e){};


                imageView.setOnClickListener(new MOnClickListener(item.indexInData));
                int spacing = (int)getResources().getDimension(R.dimen.photogrid_spacing);
                int modulo = item.indexInSection%getNumberOfColumns();
                boolean first = modulo==0;
                boolean last = modulo == getNumberOfColumns()-1;

                boolean top = item.indexInSection<getNumberOfColumns();

                int lessThan = sectionItemCounts.get(item.sectionIndex)%getNumberOfColumns();
                if(lessThan==0)
                    lessThan = getNumberOfColumns();
                boolean bottom = sectionItemCounts.get(item.sectionIndex)-(item.indexInSection+1)<lessThan;
                int leftPadding = autoContentInset?(first?spacing:0):0;
                int rightPadding = autoContentInset?(last?spacing:0):0;
                int topPadding = top?spacing:0;
                int bottomPadding = bottom?spacing:0;

                if(leftPadding==0&&rightPadding==0)
                    imageView.setHeightOffset(-spacing);
                else
                    imageView.setHeightOffset(0);

                itemView.setPadding(leftPadding,topPadding,rightPadding,bottomPadding);
            }

        }
        private class OverflowImageViewHolder extends ViewHolder{

            SquareImageView imageView;
            TextView textView;
            public OverflowImageViewHolder(View itemView) {
                super(itemView);
                this.imageView = (SquareImageView)itemView.findViewById(R.id.image_view);
                this.textView = (TextView)itemView.findViewById(R.id.text_view);

            }

            public void bind(ListItem item){
                try{
                    imageView.setImageResource(Integer.parseInt(item.data.getImageUri()));
                }catch (Exception e){};


                int countToShow = 0;
                if(item.sectionIndex<sectionMaxLabelNumbers.size()){
                    countToShow = sectionMaxLabelNumbers.get(item.sectionIndex);
                }else{
                    int sectionCount = sectionItemCounts.get(item.sectionIndex);
                    int actualCount = item.sectionIndex>=sectionMaxData.size()?0:sectionMaxData.get(item.sectionIndex);
                    countToShow = sectionCount-actualCount;
                }


                this.textView.setText("+"+countToShow);


                int spacing = (int)getResources().getDimension(R.dimen.photogrid_spacing);
                int modulo = item.indexInSection%getNumberOfColumns();
                boolean first = modulo==0;
                boolean last = modulo == getNumberOfColumns()-1;

                boolean top = item.indexInSection<getNumberOfColumns();
                int leftPadding = autoContentInset?(first?spacing:0):0;
                int rightPadding = autoContentInset?(last?spacing:0):0;
                int topPadding = top?spacing:0;

                if(leftPadding==0&&rightPadding==0)
                    imageView.setHeightOffset(-spacing);
                else
                    imageView.setHeightOffset(0);

                itemView.setPadding(leftPadding,topPadding,rightPadding,0);

            }

        }

        private class HeaderHolder extends ViewHolder{

            TextView textView;
            public HeaderHolder(View itemView) {
                super(itemView);
                this.textView = (TextView)itemView.findViewById(R.id.text_view);
            }

            public void bind(ListItem item){
                this.textView.setText(item.data.getSectionIdentifier());
            }


        }



    }


    private class HSVAdapter extends BaseAdapter {

        private static final int VIEW_TYPE_IMAGE = 1;
        private static final int VIEW_TYPE_OVERFLOW_IMAGE = 2;
        private int maxNumber = Integer.MAX_VALUE;


        private class ListItem{
            public int viewType;
            public PhotoGridDisplayable data;

            public ListItem(PhotoGridDisplayable data,int viewType){
                this.data = data;
                this.viewType = viewType;
            }
        }

        @Override
        void buildListItems(){
            if(data==null)
                return;
            items = new ArrayList<>();
            for(int i=0;i<data.size();i++){
                if(i>=maxNumber)
                    break;
                items.add(new ListItem(data.get(i),i==maxNumber-1?VIEW_TYPE_OVERFLOW_IMAGE:VIEW_TYPE_IMAGE));
            }

        }

        private ArrayList<ListItem> items;


        public HSVAdapter(){
             super();
        }


        @Override
        public int getItemViewType(int position) {
            if(items==null)
                return VIEW_TYPE_IMAGE;
            return items.get(position).viewType;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            View itemView;

            switch(viewType){
                case VIEW_TYPE_IMAGE:
                    itemView = LayoutInflater.
                            from(parent.getContext()).
                            inflate(R.layout.item_photogrid, parent, false);
                    return new ImageViewHolder(itemView);

                case VIEW_TYPE_OVERFLOW_IMAGE:
                    itemView = LayoutInflater.
                            from(parent.getContext()).
                            inflate(R.layout.item_photogrid_overflow, parent, false);
                    return new OverflowImageViewHolder(itemView);
            }

            return null;

        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, int position) {


            switch (getItemViewType(position)){
                case VIEW_TYPE_IMAGE:
                    ImageViewHolder imageViewHolder = (ImageViewHolder)viewHolder;
                    imageViewHolder.bind(items.get(position));
                    break;
                case VIEW_TYPE_OVERFLOW_IMAGE:
                    OverflowImageViewHolder overflowImageViewHolder = (OverflowImageViewHolder)viewHolder;
                    overflowImageViewHolder.bind(items.get(position));
                    break;

            }



        }



        @Override
        public int getItemCount() {
            if(items==null)
                return 0;
            return items.size();
        }

        private class ImageViewHolder extends ViewHolder{



            ImageView imageView;
            public ImageViewHolder(View itemView) {
                super(itemView);
                this.imageView = (ImageView)itemView.findViewById(R.id.image_view);
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)imageView.getLayoutParams();
                params.width = (int)getResources().getDimension(R.dimen.photogrid_hsv_size_normal);
                params.height = (int)getResources().getDimension(R.dimen.photogrid_hsv_size_normal);
                imageView.setLayoutParams(params);

            }

            public void bind(ListItem item){
                try{
                    imageView.setImageResource(Integer.parseInt(item.data.getImageUri()));
                }catch (Exception e){}

            }

        }
        private class OverflowImageViewHolder extends ViewHolder{

            ImageView imageView;
            TextView textView;
            public OverflowImageViewHolder(View itemView) {
                super(itemView);
                this.imageView = (ImageView)itemView.findViewById(R.id.image_view);
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)imageView.getLayoutParams();
                params.width = (int)getResources().getDimension(R.dimen.photogrid_hsv_size_normal);
                params.height = (int)getResources().getDimension(R.dimen.photogrid_hsv_size_normal);
                imageView.setLayoutParams(params);
                this.textView = (TextView)itemView.findViewById(R.id.text_view);
            }

            public void bind(ListItem item){
                try{
                    imageView.setImageResource(Integer.parseInt(item.data.getImageUri()));
                }catch (Exception e){};

                int countToShow = data.size()-items.size();

                this.textView.setText("+"+countToShow);


            }

        }


    }

    private class MOnClickListener implements View.OnClickListener {
        public int positionInData = Utils.INVALID_INT;
        public MOnClickListener(int positionInData){
            this.positionInData = positionInData;
        }
        @Override
        public void onClick(View v) {
            if(positionInData!= Utils.INVALID_INT&&onItemClickListener!=null){
                onItemClickListener.onItemClick(positionInData,v);
            }
        }
    }







}
