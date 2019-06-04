package com.example.talangweather;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.example.talangweather.db.City;
import com.example.talangweather.db.County;
import com.example.talangweather.db.Province;
import com.example.talangweather.gson.CitySearch;
import com.example.talangweather.util.HttpUtil;
import com.example.talangweather.util.Utility;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import com.example.talangweather.util.KeybordUtil;

import static android.content.Context.INPUT_METHOD_SERVICE;

public class ChooseAreaFragment extends Fragment {
    private FragmentVisibleListener onVisibleListener;
    private TextView titleText;
    private Button backButton;
    private  ListView listView;
    private  EditText searchText;
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNY = 2;
    private ProgressDialog progressDialog;
    private ArrayAdapter<String> adapter;
    private List<String > dataList = new ArrayList();
    private List<Province> provinceList;
    private List<City> cityList;
    private List<County> countyList;
    private List<CitySearch.HeWeather6Bean.BasicBean> searchCityList = new ArrayList<>();
    private Province selectedProvince;
    private City selectedCity;
    private int currentLevel;
    private boolean ifSearch;
    private boolean isVisible;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle saveInstanceState){

        View view = inflater.inflate(R.layout.choose_area, container, false);
        ButterKnife.bind(this,view);
        searchText=(EditText)view.findViewById(R.id.search_text);
        titleText = (TextView)view.findViewById(R.id.title_text);
        backButton = (Button)view.findViewById(R.id.back_button);
        listView = (ListView)view.findViewById(R.id.list_view);
        initView();
        return view;
    }
    @CallSuper
    protected  void initView(){

        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);

        searchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchCity(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        //搜索框获取和失去焦点关闭和打开软键盘
        searchText.setOnFocusChangeListener(new android.view.View.
                OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
             //       KeybordUtil.showSoftInput();
                } else {
                    KeybordUtil.closeKeybord(getActivity());
                }
            }
        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(currentLevel == LEVEL_PROVINCE){
                    selectedProvince = provinceList.get(position);
                    queryCities();
                }else if (currentLevel == LEVEL_CITY){
                    selectedCity = cityList.get(position);
                    queryCounties();
                }else if(currentLevel == LEVEL_COUNY){
                    String weatherId;
                    if (ifSearch){
                        weatherId = searchCityList.get(position).getCid();
                    }else {
                        weatherId   = countyList.get(position).getWeatherId();
                    }

                    if (getActivity() instanceof MyMainActivity) {
                        Intent intent = new Intent(getActivity(), WeatherActivity.class);
                        intent.putExtra("weather_Id", weatherId);
                        startActivity(intent);
                        getActivity().finish();
                    }else if(getActivity() instanceof WeatherActivity){
                        WeatherActivity weatherActivity = (WeatherActivity)getActivity();
                        weatherActivity.drawerLayout.closeDrawers();
                        weatherActivity.swipeRefreshLayout.setRefreshing(true);
                        weatherActivity.requestWeather(weatherId);
                    }
                }
            }
        });
        backButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                if (currentLevel == LEVEL_COUNY){
                    queryCities();
                }else if (currentLevel == LEVEL_CITY){
                    queryProvinces();
                }
            }
        });
        queryProvinces();
    }

    /**
     * 查询全国所有的省，优先从数据库查询，如果没有查询到再去服务器上查询
     */
    private void queryProvinces(){
        titleText.setText("中国");
        backButton.setVisibility(View.GONE);
        provinceList = DataSupport.findAll(Province.class);
        if (provinceList.size() > 0){
            dataList.clear();
            for (Province province : provinceList
                  ) {
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        }else {
            String address ="http://guolin.tech/api/china";
            queryFromServer(address,"province");
        }
    }

    /**
     * 查询省中所有市，优先从数据库查询，如果没有查询到再去服务器中查询
     */
    private void queryCities(){
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList = DataSupport.where("provinceid = ?",String.valueOf(selectedProvince.getId())).find(City.class);  //getId这里是什么操作，从未给ID赋值啊
        if (cityList.size()>0){
            dataList.clear();
           for (City city : cityList
                    ) {
                dataList.add(city.getCityName());
            }
           adapter.notifyDataSetChanged();
           listView.setSelection(0);
           currentLevel = LEVEL_CITY;
        }else {
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/"+ provinceCode;
            queryFromServer(address, "city");
        }
    }

    /**
     * 查询选中市内所有的县，优先从数据库查询，如果没有查询至再去服务器查询
     */
    private void queryCounties(){
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList = DataSupport.where("cityId = ?", String.valueOf(selectedCity.getId())).find(County.class); //这里的getId同不懂
        if (countyList.size() > 0){
            dataList.clear();
            for (County county : countyList
                  ) {
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNY;
        }else {
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china/"+provinceCode+"/"+cityCode;
            queryFromServer(address, "county");
        }
    }

    /**
     * 根据传入的地址和类型和从服务器上查询省市县数据
     */
    private void queryFromServer(String address, final String type){
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                boolean result = false;
                if ("province".equals(type)){
                    result = Utility.handleProvinceResponse(responseText);
                }else if ("city".equals(type)){
                    result = Utility.handleCityResponse(responseText, selectedProvince.getId());
                }else if ("county".equals(type)){
                    result = Utility.handleCountyResponse(responseText, selectedCity.getId());
                }
                if(result){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if("province".equals(type)){
                                queryProvinces();
                            }else if("city".equals(type)){
                                queryCities();
                            }else if("county".equals(type)){
                                queryCounties();
                            }
                        }
                    });
                }
            }
        });
    }

    /**
     * 显示进度对话框
     */
    private void showProgressDialog(){
        if (progressDialog == null){
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载数据...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    /**
     * 关闭进度对话框
     */
    private void closeProgressDialog(){
        if (progressDialog != null){
            progressDialog.dismiss();
        }
    }

    private void searchCity(String searchText){
        if (searchText == null || searchText.equals("")){
            ifSearch = false;
            queryProvinces();
        }else {
            ifSearch = true;
            dataList.clear();
            String cityUrl = "https://search.heweather.net/find?location="+searchText+"&key=0fe3257e24f848889d58733a4ea361ed&number=20";
            HttpUtil.sendOkHttpRequest(cityUrl, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Toast.makeText(getActivity(),"获取城市信息失败",Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String cityinfo = response.body().string();
                    final CitySearch cities = Utility.handleCityResponse(cityinfo);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (cities != null && "ok".equals(cities.getHeWeather6().get(0).getStatus())){
                                for (CitySearch.HeWeather6Bean.BasicBean city : cities.getHeWeather6().get(0).getBasic()){
                                    searchCityList.add(city);
                                    dataList.add(city.getLocation());
                                }
                            }
                            //只能放在RunOnUIThread里，否则会比异步查询先调用
                            adapter.notifyDataSetChanged();
                            listView.setSelection(0);
                            currentLevel = LEVEL_COUNY;
                        }
                    });
                }
            });

        }
    }

    public FragmentVisibleListener getOnVisibleListener() {
        return onVisibleListener;
    }

    public void setOnVisibleListener(FragmentVisibleListener onVisibleListener) {
        this.onVisibleListener = onVisibleListener;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (onVisibleListener != null) {
            onVisibleListener.onOpened();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (onVisibleListener != null) {
            onVisibleListener.onClosed();
        }
    }



}
