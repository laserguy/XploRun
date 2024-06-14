FROM gboeing/osmnx:latest

COPY ./requirements.txt /app/requirements.txt
WORKDIR "/app"

#COPY ./backend ./

ENV DB_PORT=3306
ENV DB_NAME='explorun'
ENV DB_USER_NAME='root'
ENV DB_SERVER='MYSQL'
ENV DB_PASSWORD='root'

RUN pip install --upgrade pip
RUN pip install -r requirements.txt

CMD ["python","endpoint.py"]