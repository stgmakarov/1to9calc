import org.mariuszgromada.math.mxparser.Expression;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.*;

public class CalcSeq extends RecursiveTask<String []>{
    public static int maxVal = 15000;

    private int jobCnt=0;
    public int maxJob = 8;
    public int calPackCnt = 1000;
    public static  int maxZeroCnt = 20;
    public static char[] operands = {'+', '-', '*', '(', ')', '^', '|'};
    public static CheckedTab chTab = new CheckedTab();
    public CalcSeq(int iJobCnt, CheckedTab chTabLoc){
        this.jobCnt = iJobCnt;
        chTab = chTabLoc;
    }

    public static void main(String[] args) throws IOException {
        String [] res = new String[maxVal];
        int voidResLoc = res.length;
        int zeroCnt = 0;
        int prevVoidRes = -1;
        boolean fl;

        ForkJoinPool forkJoinPool = new ForkJoinPool();
        do{
            String [] partRes = new String[maxVal];
            CalcSeq task = new CalcSeq(0, chTab);
            //Выполнить задачу
            Future<String []> result = forkJoinPool.submit(task);
            try {
                partRes = result.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }

            res = collect(partRes, res);
            voidResLoc = voidRes(res);
            System.out.println("Осталось пустых: "+voidResLoc);
            System.out.println("Проверено "+chTab.getSize());

            if (prevVoidRes == voidResLoc){
                zeroCnt++;
            }else zeroCnt = 0;
            prevVoidRes = voidResLoc;
            fl = (voidResLoc>0) && (zeroCnt<maxZeroCnt);
            DataSave("result.txt",res);

        }while (fl);

        for(int i = 0;i < res.length;i++){
            String str = (i+1) + ". " + res[i];
            System.out.println(str);
        }
    }

    public static void DataSave(String fpath, String[] arr) throws IOException {
        Path path = Paths.get(fpath);
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            Files.createFile(path);
            for(int i=0;i<arr.length;i++){
                Files.writeString(path, (i+1) + ". "+ arr[i] + System.lineSeparator(),StandardOpenOption.CREATE,StandardOpenOption.APPEND);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected String [] compute(){
        String [] res = new String[maxVal];
        if (jobCnt < maxJob){
            CalcSeq Task1 = new CalcSeq(jobCnt+1, chTab);
            CalcSeq Task2 = new CalcSeq(jobCnt+1, chTab);
            // Выполняем подзадачи
            Task1.fork();
            Task2.fork();
            // Ждем завершения подзадачи и получаем результат
            String [] Result1 = Task1.join();
            String [] Result2 = Task2.join();
            // Объединить подзадачи
            res = collect(Result1, Result2);
        }else{
            res = calc(calPackCnt);
        }

        return res;
    }

    private static String [] collect(String [] inp1, String [] inp2){
        for(int i=0;i<inp1.length;i++){
            if(inp1[i]==null)continue;
            String [] inpStr = new String[2];
            inpStr[0] = inp1[i];
            inpStr[1] = String.valueOf((i+1));
            addRes(inpStr, inp2);
        }
        return inp2;
    }

    private String[] calc(int packCnt){
        String [] res = new String[maxVal];
        int i = 0;
        do{
            String[] singleVal = pryamoy();
            if (singleVal[0] != null) {
                addRes(singleVal, res);
                i++;
            }
        } while (i<packCnt);
        return res;
    }

    public static int voidRes(String[] res){
        int cnt = 0;
        for (String re : res) {
            if (re == null) cnt++;
        }
        return cnt;
    }

    public static void addRes(String[] inp, String[] res){
        int val = isNumeric(inp[1]);
        if ((val >= 1) && (val <= res.length)){
            if ((res[val-1]==null)||(inp[0].length() < res[val-1].length())){
                res[val-1] = inp[0];
            }
        }
    }

    public static int isNumeric(String strNum) {
        int i;
        double d;
        if (strNum == null) {
            return 0;
        }
        try {
            d = Double.parseDouble(strNum);
        } catch (NumberFormatException nfe) {
            return 0;
        }
        if ((d % 1) == 0){
            i = (int)d;
        } else i = 0;
        return i;
    }

    private static int getPowInd(String s){
        return s.indexOf("^(");
    }

    private static boolean checkPow(String s){
        int idx = getPowInd(s);
        boolean res = true;
        if (idx > 1){
            idx+=2;
            int idx2=idx+1;
            int count = 1;
            for(int i=idx;i<s.length();i++){
                count += switch (s.charAt(i)){
                    case '(': yield 1;
                    case ')': yield -1;
                    default: yield 0;
                };
                if (count==0){
                    idx2 = i;
                    break;
                }
            }
            String subStr = s.substring(idx,idx2);
            double evalRes = eval(subStr);
            res = (evalRes<=20) && (evalRes>=0);

        }
        return res;
    }

    private String[] pryamoy() {
        String res[] = new String[2];
        char lastChar;

        //1..9, start from dig
        String s = "";
        int randomNum;
        randomNum = ThreadLocalRandom.current().nextInt(0, 2);

        s = switch (randomNum){
            case 1: yield "(";
            default: yield "";
        };
        for (int i = 1; i <= 8; i++) {
            //number
            if (s.length() >=1){
                lastChar = s.charAt(s.length() - 1);
                if (!((lastChar >= '1')&&(lastChar <= '9'))) {
                    randomNum = ThreadLocalRandom.current().nextInt(0, 2);
                    s += switch (randomNum) {
                        case 0:
                            yield i;
                        default:
                            yield "(-" + i + ")";
                    };
                }else s += i;
            }else s += i;


            //operand
            do {
                randomNum = ThreadLocalRandom.current().nextInt(0, operands.length);
                //System.out.println(s);
            } while (!checkAvOp(s, operands[randomNum]));
            s = addOpr(s, operands[randomNum]);

            //(
            randomNum = ThreadLocalRandom.current().nextInt(0, operands.length);
            if (checkAvOp(s, operands[randomNum])) {
                s = addOpr(s, operands[randomNum]);
            }

            //sign after )
            lastChar = s.charAt(s.length() - 1);
            if (lastChar == ')') {
                do {
                    randomNum = ThreadLocalRandom.current().nextInt(0, operands.length);
                    //System.out.println(s);
                } while (!checkAvOp(s, operands[randomNum]));
                s = addOpr(s, operands[randomNum]);
                randomNum = ThreadLocalRandom.current().nextInt(0, 2);
                s += switch (randomNum){
                    case 1: yield "(";
                    default: yield "";
                };
            }
        }
        s = s + '9';

        int l = calccountCloseAv(s);
        if (l > 0) {
            for (int i = 0; i < l; i++) {
                s = s + ')';
            }
        }

        res[0] = s;
        //System.out.println(s);

        if(!chTab.checkExists(res[0])) {
            if (checkPow(s)) {
                res[1] = String.valueOf(eval(s));
            } else res[1] = "NoN";
            chTab.addKey(res[0],res[1]);
        }else{
            res[0]=null;
            res[1]=null;
        }

        return res;
    }

    private static int calccountCloseAv(String s) {
        int l = s.length();
        return (l - s.replace("(", "").length()) - (l - s.replace(")", "").length());
    }

    private static String addOpr(String s, char opr) {
        if (opr != '|') {
            return s + opr;
        } else return s;
    }

    private static boolean checkAvOp(String s, char opr) {
        boolean res;
        int l = s.length();
        int countCloseAv = calccountCloseAv(s);
        boolean lastDig;
        boolean preLastDig;
        char lastChar;
        if (l > 0) {
            lastChar = s.charAt(l - 1);
            lastDig = (lastChar >= '1') && (lastChar <= '9');
            if (l>1){
                preLastDig = (s.charAt(l - 2) >= '1') && (s.charAt(l - 2) <= '9');
            }else preLastDig = false;


            res = switch (opr) {
                case '+', '-', '*', '/':
                    yield (lastDig || (lastChar == ')'));
                case '^':
                    yield (getPowInd(s) == -1) && ( (lastDig && ((! preLastDig)||(l<=3)) ) || (lastChar == ')'));
                case '(':
                    yield ((!lastDig) && (lastChar != ')'));
                case ')':
                    yield lastDig && (countCloseAv > 0);
                case '|':
                    yield (lastDig);
                default:
                    yield false;
            };
            if (res && opr == '^' && l>2){
                res = (((s.charAt(l-1) >= '1')&&(s.charAt(l-1) <= '6'))||(s.charAt(l-1)==')')) && (s.charAt(l-2) != '^') && (s.charAt(l-3) != '^');
            }
        } else {
            res = (opr == '(' || opr == '-');
        }
        return res;
    }

    private static double eval(String s) {
        Expression e = new Expression(s);
        return e.calculate();
    }

}