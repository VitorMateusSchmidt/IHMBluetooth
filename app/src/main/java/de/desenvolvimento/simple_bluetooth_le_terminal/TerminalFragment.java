package de.desenvolvimento.simple_bluetooth_le_terminal;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import de.desenvolvimento.simple_bluetooth_le_terminal.ui.CRC16Calculator;

public class TerminalFragment extends Fragment implements ServiceConnection, SerialListener {

    private enum Connected {False, Pending, True}

    private String deviceAddress;
    private SerialService service;

    private TextView receiveText;
//    private TextView sendText;
    private TextUtil.HexWatcher hexWatcher;

    private Spinner spinner_var;
    private TextView writeNumber;

    private ProgressBar progressBarTensao;
    private ProgressBar progressBarFreq;
    private ProgressBar progressBarTensaoSetada;
    private ProgressBar progressBarCorrente;
    private ProgressBar progressBarDuty;

    private TextView textTensaoBar;
    private TextView textFreqBar;
    private TextView textTensaoSetBar;
    private TextView textCorrenteBar;
    private TextView textDuty;
    private TextView textFormaOnda;

    private int flagEndVarL;
    private int flagQtdL;

    private Connected connected = Connected.False;
    private boolean initialStart = true;
    private boolean hexEnabled = false;
    private boolean pendingNewline = false;
    private String newline = TextUtil.newline_crlf;

    private static byte[] vetorDeRecepcao;
    private int qtdEsperadaRecepcao;
    private int indexDeRecepcao;

    /*
     * Lifecycle
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        deviceAddress = getArguments().getString("device");
    }

    @Override
    public void onDestroy() {
        if (connected != Connected.False)
            disconnect();
        getActivity().stopService(new Intent(getActivity(), SerialService.class));
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (service != null)
            service.attach(this);
        else
            getActivity().startService(new Intent(getActivity(), SerialService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change
    }

    @Override
    public void onStop() {
        if (service != null && !getActivity().isChangingConfigurations())
            service.detach();
        super.onStop();
    }

    @SuppressWarnings("deprecation")
    // onAttach(context) was added with API 23. onAttach(activity) works for all API versions
    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        getActivity().bindService(new Intent(getActivity(), SerialService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDetach() {
        try {
            getActivity().unbindService(this);
        } catch (Exception ignored) {
        }
        super.onDetach();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onResume() {
        super.onResume();
        if (initialStart && service != null) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
        service.attach(this);
        if (initialStart && isResumed()) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }

    /**
     * UI
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terminal, container, false);
        receiveText = view.findViewById(R.id.receive_text);                          // TextView performance decreases with number of spans
        receiveText.setTextColor(getResources().getColor(R.color.colorRecieveText)); // set as default color to reduce number of spans
        receiveText.setMovementMethod(ScrollingMovementMethod.getInstance());

        textTensaoBar = view.findViewById(R.id.textTensaoBar);
        textFreqBar = view.findViewById(R.id.textFreqBar);
        textTensaoSetBar = view.findViewById(R.id.textTensaoSetBar);
        textCorrenteBar = view.findViewById(R.id.textCorrenteBar);
        textDuty = view.findViewById(R.id.textDutyCycle);
        textFormaOnda = view.findViewById(R.id.textFormaOnda);

        progressBarTensao = view.findViewById(R.id.progressBarTensao);
        progressBarFreq = view.findViewById(R.id.progressBarFreq);
        progressBarTensaoSetada = view.findViewById(R.id.progressBarTensaoSet);
        progressBarCorrente = view.findViewById(R.id.progressBarCorrente);
        progressBarDuty = view.findViewById(R.id.progressBarDuty);

        writeNumber = view.findViewById(R.id.write_number);




//        sendText = view.findViewById(R.id.send_text);
//        hexWatcher = new TextUtil.HexWatcher(sendText);
//        hexWatcher.enable(hexEnabled);
//        sendText.addTextChangedListener(hexWatcher);
//        sendText.setHint(hexEnabled ? "HEX mode" : "");

//        View sendBtn = view.findViewById(R.id.send_btn);
//        sendBtn.setOnClickListener(v -> send(sendText.getText().toString()));
//
//        View btnSend = view.findViewById(R.id.btn_send);
//        btnSend.setOnClickListener(v -> send(sendText.getText().toString()));

//        View sendBtn = view.findViewById(R.id.send_btn);
//        sendBtn.setOnClickListener(v -> request_data());

        View btnSend = view.findViewById(R.id.btn_send);
        btnSend.setOnClickListener(v -> request_data());

        View btnWrite = view.findViewById(R.id.btn_write);
        btnWrite.setOnClickListener(v -> btnWriteClick());

        spinner_var = view.findViewById(R.id.spinner_variavel);
        //Cria um ArrayAdapter usando o array de string e um layout padrão
        ArrayAdapter<CharSequence> adapterVar = ArrayAdapter.createFromResource(this.getActivity(), R.array.variaveis_array, android.R.layout.simple_spinner_dropdown_item);
        //especifica o layout a ser usado quando a lista aparece
        adapterVar.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //aplica o adaptador ao spinner
        spinner_var.setAdapter(adapterVar);

        Spinner spinner_adr = view.findViewById(R.id.spinner_adress);
        ArrayAdapter<CharSequence> adapterAdr = ArrayAdapter.createFromResource(this.getActivity(), R.array.variaveis_adress, android.R.layout.simple_spinner_dropdown_item);
        adapterAdr.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_adr.setAdapter(adapterAdr);

        return view;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_terminal, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.connect) {
            try {
                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
                status("conectando...");
//                Toast.makeText(getActivity(), "conectando...", Toast.LENGTH_SHORT).show();
                connected = Connected.Pending;
                SerialSocket socket = new SerialSocket(getActivity().getApplicationContext(), device);
                service.connect(socket);
            } catch (Exception e) {
                onSerialConnectError(e);
            }
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Serial + UI
     */
    private void connect() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            status("conectando...");
//            Toast.makeText(getActivity(), "conectando...", Toast.LENGTH_LONG).show();
            connected = Connected.Pending;
            SerialSocket socket = new SerialSocket(getActivity().getApplicationContext(), device);
            service.connect(socket);
        } catch (Exception e) {
            onSerialConnectError(e);
        }
    }

    private void disconnect() {
        connected = Connected.False;
        service.disconnect();
    }

    private void btnWriteClick() {
        byte enderecoVariavelEscritaBaixo = 0;
        int valorEscrita;
        valorEscrita = Integer.parseInt(writeNumber.getText().toString());

        byte valorEscritaAlto = 0;
        byte valorEscritaBaixo = 0;

        switch ((int) spinner_var.getSelectedItemId()) {
            case 0://Tensão
                enderecoVariavelEscritaBaixo = (byte) 23;
                valorEscritaAlto = (byte) ((valorEscrita >> 8) & 0xffL);
                valorEscritaBaixo = (byte) (valorEscrita & 0xFF);
                break;
            case 1://Frequência
                enderecoVariavelEscritaBaixo = (byte) 14;
                valorEscritaAlto = (byte) ((valorEscrita >> 8) & 0xffL);
                valorEscritaBaixo = (byte) (valorEscrita & 0xFF);
                break;
            case 2://Forma de onda
                enderecoVariavelEscritaBaixo = (byte) 30;
                valorEscritaAlto = (byte) ((valorEscrita >> 8) & 0xffL);
                valorEscritaBaixo = (byte) (valorEscrita & 0xffL);
                break;
            case 3://Duty cycle
                enderecoVariavelEscritaBaixo = (byte) 15;
                valorEscritaBaixo = (byte) (valorEscrita & 0xffL);
                break;
            case 4://Ajuste Tensão
                enderecoVariavelEscritaBaixo = (byte) 38;
                valorEscritaBaixo = (byte) (valorEscrita & 0xffL);
                break;
            case 5://Ajuste Corrente
                enderecoVariavelEscritaBaixo = (byte) 39;
                valorEscritaBaixo = (byte) (valorEscrita & 0xffL);
                break;
            case 6://Ajuste Frequencia
                enderecoVariavelEscritaBaixo = (byte) 42;
                valorEscritaBaixo = (byte) (valorEscrita & 0xffL);
                break;
            default:
                enderecoVariavelEscritaBaixo = (byte) 0;
                break;
        }
        write_data(enderecoVariavelEscritaBaixo, valorEscritaAlto, valorEscritaBaixo);
    }


    public void request_data() {
        byte enderecoModbus = 1;
        byte funcaoModbus = 3;
        byte enderecoVariavelBaixo = 0;
        byte quantidadeDadosAlto = 0;
        byte quantidadeDadosBaixo = 31;
        flagEndVarL = enderecoVariavelBaixo;
        send(enderecoModbus, funcaoModbus, enderecoVariavelBaixo, quantidadeDadosAlto, quantidadeDadosBaixo);

    }

    public void request_data(byte endVarL) {
        byte enderecoModbus = 1;
        byte funcaoModbus = 3;
        byte enderecoVariavelBaixo = endVarL;
        byte quantidadeDadosAlto = 0;
        byte quantidadeDadosBaixo = 7;
        flagEndVarL = enderecoVariavelBaixo;
        send(enderecoModbus, funcaoModbus, enderecoVariavelBaixo, quantidadeDadosAlto, quantidadeDadosBaixo);
    }

    public void write_data(byte endVarL, byte valorAlto, byte valorBaixo) {
        byte enderecoModbus = 1;
        byte funcaoModbus = 6;
        byte enderecoVariavelBaixo = endVarL;
        byte valorEscritaAlto = valorAlto;
        byte ValorEscritaBaixo = valorBaixo;
        flagEndVarL = enderecoVariavelBaixo;
        send(enderecoModbus, funcaoModbus, enderecoVariavelBaixo, valorEscritaAlto, ValorEscritaBaixo);

    }

    public void send(byte enderecoModbus, byte funcaoModbus, byte enderecoVariavelBaixo, byte data04, byte data05) { //private void send(String str) { byte quantidadeDadosBaixo
        if (connected != Connected.True) {
//            Toast.makeText(getActivity(), "não conectado", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String msg = "";
            byte[] data = {0, 0, 0, 0, 0, 0, 0, 0};

            data[0] = enderecoModbus;
            data[1] = funcaoModbus;
            data[2] = 0;
            data[3] = enderecoVariavelBaixo;
            data[4] = data04;//Qtd alta de requisição
            data[5] = data05;//Qtd baixa de requisição

            byte crc16[] = CRC16Calculator.calculateCRC16(data);
            data[6] = crc16[0];
            data[7] = crc16[1];

//            msg = data[0] + " " + data[1] + " 0 " + data[3] + " 0 " + data[5] + " " + data[6] + " " + data[7];
            for (int i = 0;i < 8;i++){
                msg = msg + (data[i] & 0xffL) + " ";
            }

//            SpannableStringBuilder spn = new SpannableStringBuilder(msg + '\n');
//            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSendText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//            receiveText.append(spn);
            service.write(data);
            if(data[1] == 0x03){
                qtdEsperadaRecepcao = data[5]*2 + 5;
                vetorDeRecepcao = new byte[qtdEsperadaRecepcao];
            }
            indexDeRecepcao = 0;

        } catch (Exception e) {
            onSerialIoError(e);
        }
    }


    private void receive(byte[] data) {

        int end = data.length;
        int begin = 0;

        if(indexDeRecepcao <= qtdEsperadaRecepcao && data.length > 0 && (data[1] == 0x03 || vetorDeRecepcao[1] == 0x03)){
            for (int pos = begin; pos < end; pos++) {
                vetorDeRecepcao[indexDeRecepcao] = data[pos];
                indexDeRecepcao++;
            }
        }
        if(indexDeRecepcao >= qtdEsperadaRecepcao && (data[1] == 0x03 || vetorDeRecepcao[1] == 0x03)){
            //verificar crc
            trataDadosRecebidos();
        }

        if(data[1] == 0x06){
            Toast.makeText(getActivity(), "Valor alterado!", Toast.LENGTH_LONG).show();
        }
    }

    private void trataDadosRecebidos(){
        if (vetorDeRecepcao[0] == 0x01 && vetorDeRecepcao[1] == 0x03 && flagEndVarL == 0) {
            long tensaoLida = ((vetorDeRecepcao[3] & 0xffL) * 256 + (vetorDeRecepcao[4] & 0xffL));

            double correnteSaida = ((vetorDeRecepcao[5] & 0xffL) * 256 + (vetorDeRecepcao[6] & 0xffL)) / 100.0;

            long dcLink = ((vetorDeRecepcao[7] & 0xffL) * 256 + (vetorDeRecepcao[8] & 0xffL));

            long frequenciaSaida = ((vetorDeRecepcao[31] & 0xffL) * 256 + (vetorDeRecepcao[32] & 0xffL));

            long tensaoSetada = ((vetorDeRecepcao[49] & 0xffL) * 256 + (vetorDeRecepcao[50] & 0xffL));

            long dutyCycle = ((vetorDeRecepcao[33] & 0xffL) * 256 + (vetorDeRecepcao[34] & 0xffL));

            long formaDeOnda = ((vetorDeRecepcao[63] & 0xffL) * 256 + (vetorDeRecepcao[64] & 0xffL));

            progressBarTensao.setProgress((int) tensaoLida);
            textTensaoBar.setText("Tensão lida: " + tensaoLida + " Volts");

            progressBarFreq.setProgress((int) frequenciaSaida);
            textFreqBar.setText("Frequencia na saida: " + frequenciaSaida + " Hz");

            progressBarTensaoSetada.setProgress((int) tensaoSetada);
            textTensaoSetBar.setText("Tensão setada: " + tensaoSetada + " Volts");

            progressBarCorrente.setProgress((int) correnteSaida * 100);
            textCorrenteBar.setText("Corrente na saída: " + correnteSaida + " mA");

            progressBarDuty.setProgress((int) dutyCycle);
            textDuty.setText("Largura de Pulso: "  + dutyCycle + " %");

            switch ((int) formaDeOnda) {
                case 0://CA
                    textFormaOnda.setText("Forma de Onda: CA");
                    break;
                case 1://CC
                    textFormaOnda.setText("Forma de Onda: CC");
                    break;
                case 2://senoidal
                    textFormaOnda.setText("Forma de Onda: Senoidal");
                    break;
                case 3://CA Hibrida
                    textFormaOnda.setText("Forma de Onda: CA Híbrida");
                    break;
                case 4://CC Hibrida
                    textFormaOnda.setText("Forma de Onda: CC Híbrida");
                    break;
                case 5://CA pós-sangria
                    textFormaOnda.setText("Forma de Onda: CA Pós-Sangria");
                    break;
                case 6://CC pós-sangria
                    textFormaOnda.setText("Forma de Onda: CC Pós-Sangria");
                    break;

                default:
            }
        }
    }

    private void status(String str) {
//        SpannableStringBuilder spn = new SpannableStringBuilder(str + '\n');
//        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//        receiveText.append(spn);
        Toast.makeText(getActivity(), str, Toast.LENGTH_SHORT).show();
    }

    /**
     * SerialListener
     */
    @Override
    public void onSerialConnect() {
        status("conectado");
        connected = Connected.True;
    }

    @Override
    public void onSerialConnectError(Exception e) {
        status("falha na conexão: " + e.getMessage());
        disconnect();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onSerialRead(byte[] data) {
        receive(data);
    }

    @Override
    public void onSerialIoError(Exception e) {
        status("conexão perdida: " + e.getMessage());
        disconnect();
    }


}
