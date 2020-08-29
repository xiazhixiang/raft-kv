package org.nuist.rpc.test.utils;

import java.util.*;

public class Main{

    public static void main(String[] args){

        Scanner sc = new Scanner(System.in);
        int x = sc.nextInt();
        Map<Integer, Integer> res = new HashMap<>();
        for(int i = 1; i <= Math.pow(2, 32); i++){
            if(i >= x)
                break;
            for(int j = 1; j <= Math.pow(2, 32); j++){
                int cnt = 3;
                int a = i, b = j;
                int c = a + b;
                while(c < x){
                    cnt++;
                    a = b;
                    b = c;
                    c = a + b;
                }
                if(a + b == x){
                    res.put(cnt, res.getOrDefault(cnt, 0) + 1);
                    System.out.println(a + " " + b);
                }


                else
                    break;
            }
        }
        for(int key : res.keySet()){
            System.out.println(key + " " + res.get(key));
        }
    }
}
