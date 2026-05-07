FROM mcr.microsoft.com/playwright:v1.40.0-focal
WORKDIR /app
COPY package.json ./
RUN npm install
COPY server.js ./
EXPOSE 3000
CMD ["node", "server.js"]
