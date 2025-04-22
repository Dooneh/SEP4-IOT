#Base Image
FROM openjdk:21-alpine

#Working directory i containeren
WORKDIR /dev

#Kopier requirements
//COPY requirements.txt /app/

#installer python dependencies
//RUN pip install --no-cache-dir -r requirements.txt

#Kopier resten af projektet
COPY .

#Åbner bash
CMD ["bash"]

