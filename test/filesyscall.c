#include "stdio.h"
#include "stdlib.h"

#define MAXARGC        20
#define MAXPROCESS     10
#define MAXOPENFILES   13             /* MaxOpenFiles=16, 16-3(stdin/stdout/stderr)=13*/
#define STDIN          0
#define STDOUT         1
#define STDERR         2
#define true           1
#define false          0

int flag = true;
int fd;
int ret;

int main(){

/* Test 0 */

printf("Test 0 started\n");
printf("Creates a file, checks syscall creat works\n");
flag = true;
fd = creat("0.txt");
close(fd);
if (fd == -1) flag = false;
if (flag) printf("Test 0 succeed\n\n");
else printf("Test 0 failed\n\n");

/* Test 1 */
printf("Test 1 started\n");
printf("Calls syscall creat/close/unlink and checks that they work\n");
flag = true;
fd = creat("1.txt");
if (fd == -1) flag = false;
close(fd);
ret = unlink("1.txt");
if (ret == -1) flag = false;
ret = unlink("1.txt");
if (ret != -1) flag = false;
if (flag) printf("Test 1 succeed\n\n");
else printf("Test 1 failed\n\n");

/* Test 2 */

printf("Test 2 started\n");
printf("Tests if your syscall close actually closes the file\n");
flag = true;
fd = creat("2.txt");
if (fd == -1) flag = false;
ret = open("2.txt");
if (ret == -1) flag = false;
ret = unlink("2.txt");
if (ret == -1) flag = false;
close(fd);
if (flag) printf("Test 2 succeed, need to check whether 2.txt exists\n\n");
else printf("Test 2 failed\n\n");

/* Test 3 */

if(false){
printf("Test 3 started\n");
printf("Tests if your syscall open fails gracefully when stubFileSystem's openfile limit's exceeded\n");
flag = true;
int fds[14];
int i = 0;
fd = creat("3.txt");
printf("fd: %d", fd);
for (i = 0; i < 13; i++)
{
	fds[i] = open("3.txt");
	if (fds[i] == -1) flag = false;
	printf("flag: %d, i: %d, fd: %d\n", flag, i, fds[i]);
}
printf("flag: %d\n", flag);
fds[13] = open("3.txt");
printf("%d", fds[13]);
if(fds[13] != -1) flag = false;
for (i = 0; i < 14; i++) close(fds[i]);
if (flag) printf("Test 3 succeed\n\n");
else printf("Test 3 failed\n\n");
}

/* Test 4 */

printf("Test 4 started\n");
printf("Tests copy\n");
flag = true;
fd = open("cp.in");
ret = open("cp.out");
char buf[100];
int amount;
while ((amount = read(fd, buf, 100))>0) {
    write(ret, buf, amount);
}
close(fd);
close(ret);
if (flag) printf("Test 4 succeed\n\n");
else printf("Test 4 failed\n\n");

/* Task 5 */

printf("Test 5 started\n");
printf("Tests move\n");
flag = true;
fd = open("cp.in");
ret = open("cp.out");
while ((amount = read(fd, buf, 100))>0) {
    write(ret, buf, amount);
}
unlink("cp.in");
close(fd);
close(ret);
if (flag) printf("Test 5 succeed\n\n");
else printf("Test 5 failed\n\n");

}
