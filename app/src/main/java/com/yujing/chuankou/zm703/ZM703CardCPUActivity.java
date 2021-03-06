
package com.yujing.chuankou.zm703;

import android.annotation.SuppressLint;
import android.util.Log;

import com.yujing.chuankou.BaseActivity;
import com.yujing.chuankou.R;
import com.yujing.chuankou.databinding.ActivityZm703CpuBinding;
import com.yujing.utils.YConvert;
import com.yujing.utils.YConvertBytes;
import com.yujing.yserialport.YSerialPort;

import java.nio.charset.StandardCharsets;

/**
 * zm703读卡器 读取cpu区
 *
 * @author yujing 2019年12月3日16:18:35
 */
@SuppressLint("SetTextI18n")
public class ZM703CardCPUActivity extends BaseActivity<ActivityZm703CpuBinding> {
    YSerialPort ySerialPort;
    CpuDataListener cpuDataListener = new CpuDataListener();

    @Override
    protected Integer getContentLayoutId() {
        return R.layout.activity_zm703_cpu;
    }

    @Override
    protected void initData() {
        ySerialPort = new YSerialPort(this);
        ySerialPort.clearDataListener();
        ySerialPort.start();
        binding.btCardCpu.setOnClickListener(v -> readCpu());
        binding.btDyk.setOnClickListener(v -> show("未开发"));
        binding.tvTips.setText(String.format("注意：当前串口：%s，当前波特率：%s。\t\tZM703读卡器：\t/dev/ttyS4\t波特率115200", ySerialPort.getDevice(), ySerialPort.getBaudRate()));
    }

    /**
     * 读CPU
     */
    private void readCpu() {
        cpuDataListener.step = 0;//步骤设置成0
        ySerialPort.clearDataListener();
        ySerialPort.addDataListener(cpuDataListener);
        byte[] cmd = SerialCpu.getComplete(SerialCpu.getCommandSearch());
        Log.d("发送串口命令", YConvert.bytesToHexString(cmd));
        binding.tvResult.setText("开始寻卡\n发送串口命令:" + YConvert.bytesToHexString(cmd));
        ySerialPort.setLengthAndTimeout(7, 10);
        ySerialPort.send(cmd);
    }

    /**
     * cpu读卡监听
     */
    class CpuDataListener implements YSerialPort.DataListener {
        int step = 0;
        int packetsLength;

        @Override
        public void onDataReceived(String hexString, byte[] bytes, int size) {
            binding.tvResult.setText(binding.tvResult.getText() + "\n收到数据：" + hexString);
            Log.d("收到数据", hexString);
            ZM703 zm703 = new ZM703(hexString, bytes, size);
            Log.d("收到数据", zm703.toString());
            if (!zm703.isStatus()) {
                binding.tvResult.setText(binding.tvResult.getText() + "\n状态:失败");
                return;
            }
            binding.tvResult.setText(binding.tvResult.getText() + "\nvalue:" + zm703.getDataHexString());
            step++;
            //判断是否是自动寻卡成功，寻卡成功数据区长度为7，总长度14，启动自动寻卡数据区长度为0，总长度为7。所以，当数据区长度为7，或者，长度为0但是总长度为14+7
            if (step == 1 && !(zm703.getDataSize() == 7 || (zm703.getDataSize() == 0 && zm703.getSize() == 21))) {
                step = 0;
            }
            if (step == 1) {
                step1();
            } else if (step == 2) {
                step2();
            } else if (step == 3) {
                if ("9000".equals(zm703.getDataHexString().substring(zm703.getDataHexString().length() - 4)))
                    step3();
                else {
                    binding.tvResult.setText(binding.tvResult.getText() + "\n选择DF失败");
                    step = 0;
                }
            } else if (step == 4) {
                if ("9000".equals(zm703.getDataHexString())) step4();
                else {
                    binding.tvResult.setText(binding.tvResult.getText() + "\n复合认证失败");
                    step = 0;
                }
            } else if (step == 5) {
                if ("9000".equals(zm703.getDataHexString())) step5();
                else {
                    binding.tvResult.setText(binding.tvResult.getText() + "\n选择文件失败");
                    step = 0;
                }
            } else if (step == 6) {
                //分包
                packetsLength = YConvertBytes.bytes2ToInt(zm703.getDataBytes());
                step6(packetsLength);
            } else if (step == 7) {
                byte[][] packets = new byte[packetsLength][11];
                for (int i = 0; i < packetsLength; i++) {
                    System.arraycopy(zm703.getDataBytes(), i * 11, packets[i], 0, 11);
                }
                for (int i = 0; i < packetsLength; i++) {
                    byte[] item = packets[i];
                    binding.tvResult.setText(binding.tvResult.getText() + "\n烟包ID：" + i + "：" + YConvert.bytesToHexString(item) + "------解析----->" + new String(item, StandardCharsets.US_ASCII));
                }
            }
        }
    }

    protected void step1() {
        byte[] cmd = SerialCpu.getComplete(SerialCpu.getCpuInto());
        Log.d("发送串口命令", YConvert.bytesToHexString(cmd));
        binding.tvResult.setText(binding.tvResult.getText() + "\nCPU转入\n发送串口命令:" + YConvert.bytesToHexString(cmd));
        ySerialPort.setLengthAndTimeout(17 + 7, 10);
        ySerialPort.send(cmd);
    }

    protected void step2() {
        byte[] cmd = SerialCpu.getComplete(SerialCpu.getCos(SerialCpu.cosSelectDf()));
        Log.d("发送串口命令", YConvert.bytesToHexString(cmd));
        binding.tvResult.setText(binding.tvResult.getText() + "\n选择DF\n发送串口命令:" + YConvert.bytesToHexString(cmd));
        ySerialPort.setLengthAndTimeout(20 + 7, 10);
        ySerialPort.send(cmd);
    }

    protected void step3() {
        byte[] cmd = SerialCpu.getComplete(SerialCpu.getAuthentication());
        Log.d("发送串口命令", YConvert.bytesToHexString(cmd));
        binding.tvResult.setText(binding.tvResult.getText() + "\n复合认证\n发送串口命令:" + YConvert.bytesToHexString(cmd));
        ySerialPort.setLengthAndTimeout(2 + 7, 10);
        ySerialPort.send(cmd);
    }

    protected void step4() {
        byte[] cmd = SerialCpu.getComplete(SerialCpu.getCos(SerialCpu.cosSelectFile()));
        Log.d("发送串口命令", YConvert.bytesToHexString(cmd));
        binding.tvResult.setText(binding.tvResult.getText() + "\n选择文件\n发送串口命令:" + YConvert.bytesToHexString(cmd));
        ySerialPort.setLengthAndTimeout(2 + 7, 10);
        ySerialPort.send(cmd);
    }

    //读取长度
    protected void step5() {
        byte[] cmd = SerialCpu.getComplete(SerialCpu.readFile16k("0000", "0002"));
        Log.d("发送串口命令", YConvert.bytesToHexString(cmd));
        binding.tvResult.setText(binding.tvResult.getText() + "\n读文件\n发送串口命令:" + YConvert.bytesToHexString(cmd));
        ySerialPort.setLengthAndTimeout(2 + 7, 10);
        ySerialPort.send(cmd);
    }

    protected void step6(int packetsLength) {
        int startIndex = 11 + 2;//开始位置2位长度位+11个基本属性位
        int length = packetsLength * 11;//长度位=烟包*11
        byte[] cmd = SerialCpu.getComplete(SerialCpu.readFile16k(YConvert.bytesToHexString(YConvertBytes.intTo2Bytes(startIndex)), YConvert.bytesToHexString(YConvertBytes.intTo2Bytes(length))));
        ySerialPort.setLengthAndTimeout(length, length / 9);
        Log.d("发送串口命令", YConvert.bytesToHexString(cmd));
        binding.tvResult.setText(binding.tvResult.getText() + "\n读文件\n发送串口命令:" + YConvert.bytesToHexString(cmd));
        ySerialPort.send(cmd);
    }

    //退出注销
    @Override
    protected void onDestroy() {
        ySerialPort.onDestroy();
        super.onDestroy();
    }
}
